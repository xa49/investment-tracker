package app.data.fx.rate;

import app.data.fx.mnb_access.MNBQueryService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class ExchangeRateServiceMnb implements ExchangeRateService {
    private static final int QUERY_NUMBER_OF_DAYS = 30;
    private final ExchangeRateRepository exchangeRateRepository;
    private final MNBQueryService mnbQueryService;

    @Override
    public Optional<Rate> getExchangeRateDetails(
            String sourceCurrency, String destinationCurrency, LocalDate targetDate) {
        return getExchangeRateDetails(sourceCurrency, destinationCurrency, targetDate, 0);
    }

    @Override
    public Optional<Rate> getExchangeRateDetails(
            String sourceCurrency, String destinationCurrency, LocalDate targetDate, int backwardDayOffsetTolerance) {
        sourceCurrency = sourceCurrency.toUpperCase();
        destinationCurrency = destinationCurrency.toUpperCase();

        if (sourceCurrency.equals(destinationCurrency)) {
            return Optional.of(sameCurrencyRate(sourceCurrency, targetDate));
        }

        Map<String, Rate> hufRates =
                getHufRatesForSameDay(targetDate, backwardDayOffsetTolerance, sourceCurrency, destinationCurrency);
        Rate sourceToHuf = hufRates.get(sourceCurrency);
        Rate destinationToHuf = hufRates.get(destinationCurrency);
        if (sourceToHuf != null && destinationToHuf != null) {
            return Optional.of(mergeRates(sourceToHuf, destinationToHuf));
        } else {
            return Optional.empty();
        }
    }

    private Rate sameCurrencyRate(String currency, LocalDate targetDate) {
        return new StoredRate(currency, currency, targetDate, BigDecimal.ONE);
    }

    private Map<String, Rate> getHufRatesForSameDay(LocalDate targetDate,
                                                    int backwardDayOffsetTolerance,
                                                    String currencyAbbreviation,
                                                    String... otherCurrencyAbbreviations) {
        Set<String> currencyAbbreviations = new HashSet<>(Arrays.asList(otherCurrencyAbbreviations));
        currencyAbbreviations.add(currencyAbbreviation);
        return getHufRatesForSameDay(targetDate, backwardDayOffsetTolerance, currencyAbbreviations, 0);
    }

    private Map<String, Rate> getHufRatesForSameDay(LocalDate targetDate, int backwardDayOffsetTolerance,
                                                    Set<String> currencyAbbreviations, int dayOffsetIteration) {
        if (dayOffsetIteration > backwardDayOffsetTolerance) {
            return new HashMap<>();
        }

        LocalDate queriedDate = targetDate.minusDays(dayOffsetIteration);
        Map<String, Rate> rateToHuf = getHufRatesForDay(currencyAbbreviations, queriedDate);
        if (rateToHuf.size() == currencyAbbreviations.size()) {
            return rateToHuf;
        }

        if (dayOffsetIteration == 0) {
            Map<String, Rate> mnbResult =
                    getMissingRatesFromMnb(targetDate, currencyAbbreviations, rateToHuf.keySet(), backwardDayOffsetTolerance);
            rateToHuf.putAll(mnbResult);
            if (rateToHuf.size() == currencyAbbreviations.size()) {
                return rateToHuf;
            }
        }
        return getHufRatesForSameDay(targetDate, backwardDayOffsetTolerance, currencyAbbreviations, dayOffsetIteration + 1);
    }

    private Map<String, Rate> getHufRatesForDay(Set<String> currencyAbbreviations, LocalDate queriedDate) {
        return currencyAbbreviations.stream()
                .map(abbrev -> getHufRate(abbrev, queriedDate))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(
                        Rate::getSourceIsoAbbreviation,
                        r -> r
                ));
    }

    private Map<String, Rate> getMissingRatesFromMnb(
            LocalDate targetDate, Set<String> requiredRates, Set<String> existingRates, int backwardDayOffsetTolerance) {
        log.info("Getting missing rates from MNB {} - {}", requiredRates, targetDate);
        Set<String> missingRates = new HashSet<>(requiredRates);
        missingRates.removeAll(existingRates);

        LocalDate startDate = targetDate.minusDays(Math.max(QUERY_NUMBER_OF_DAYS, backwardDayOffsetTolerance));
        return missingRates.stream()
                .map(abbrev -> getExchangeRateFromExtendedMnbQuery(abbrev, startDate, targetDate))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(
                        Rate::getSourceIsoAbbreviation,
                        r -> r
                ));
    }

    private Optional<Rate> getExchangeRateFromExtendedMnbQuery(
            String currency, LocalDate startDate, LocalDate targetDate) {
        Map<LocalDate, BigDecimal> singleCurrencyRates =
                mnbQueryService.getHufRates(currency, startDate, targetDate);
        saveRates(currency, "HUF", singleCurrencyRates);
        return getHufRate(currency, targetDate);
    }

    private void saveRates(
            String sourceAbbreviation, String destinationAbbreviation, Map<LocalDate, BigDecimal> rates) {
        rates.forEach((date, rateValue) -> {
            if (!exchangeRateRepository.isAlreadyStored(sourceAbbreviation, destinationAbbreviation, date)) {
                StoredRate rate = new StoredRate(sourceAbbreviation, destinationAbbreviation, date, rateValue);
                exchangeRateRepository.save(rate);
            }
        });
    }

    private Optional<Rate> getHufRate(String source, LocalDate date) {
        if (source.equalsIgnoreCase("HUF")) {
            return Optional.of(sameCurrencyRate("HUF", date));
        }
        return exchangeRateRepository.findRate(source, "HUF", date);
    }

    private Rate mergeRates(Rate source, Rate destination) {
        if (source == null || destination == null) {
            throw new IllegalStateException("Rates being merged should not be null: " + source + " | " + destination);
        }
        if (!source.getDestinationIsoAbbreviation().equals(destination.getDestinationIsoAbbreviation())) {
            throw new IllegalArgumentException(
                    "Cannot merge exchange rates with different bases:\nSource: " + source + "\nDestination:"
                            + destination);
        }
        return new StoredRate(source.getSourceIsoAbbreviation(), destination.getSourceIsoAbbreviation(), source.getDate(),
                source.getExchangeRate().divide(destination.getExchangeRate(), MathContext.DECIMAL64));
    }
}
