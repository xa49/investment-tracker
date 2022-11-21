package app.data.securities;

import app.data.securities.price.SecurityPrice;
import app.data.securities.price.SecurityPriceOfDay;
import app.data.securities.price.SecurityPriceOfDayRepository;
import app.data.securities.security.Security;
import app.data.securities.security.SecurityRepository;
import app.data.securities.yahoo_access.RemoteSecurityService;
import app.util.MissingDataException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@AllArgsConstructor
@Slf4j
public class SecurityServiceImpl implements SecurityService {

    private static final int DAYS_ADDED_PER_REMOTE_REQUEST = 30;
    private final SecurityPriceOfDayRepository priceRepository;
    private final SecurityRepository securityRepository;
    private final RemoteSecurityService remoteSecurityService;

    @Override
    public Optional<SecurityPrice> getPrice(String ticker, LocalDate date) {
        return getPrice(ticker, date, 0);
    }

    @Override
    public Optional<SecurityPrice> getPrice(String ticker, LocalDate date, int backwardDaysOffsetTolerance) {
        try {
            verifyTickerIsPresent(ticker);
        } catch (MissingDataException e) {
            log.debug(e.getMessage());
            return Optional.empty();
        }

        Optional<SecurityPrice> storedPrice = priceRepository.findByTickerAndDate(ticker, date);
        if (storedPrice.isPresent()) {
            return storedPrice;
        }

        Optional<SecurityPrice> queriedPrice =
                getPriceFromExtendedRemoteSearch(ticker,
                        date.minusDays(Math.max(DAYS_ADDED_PER_REMOTE_REQUEST, backwardDaysOffsetTolerance)), date);
        if (queriedPrice.isPresent()) {
            return queriedPrice;
        }
        for (int i = 1; i <= backwardDaysOffsetTolerance; i++) {
            LocalDate queried = date.minusDays(i);
            storedPrice = priceRepository.findByTickerAndDate(ticker, queried);
            if (storedPrice.isPresent()) {
                return storedPrice;
            }
        }
       return Optional.empty();
    }

    @Override
    public Optional<String> getMarketBySecurityId(Long securityId) {
        return securityRepository.getMarketById(securityId);
    }

    @Override
    public Optional<String> getMarketByTicker(String ticker) {
        return securityRepository.getMarketByTicker(ticker);
    }

    @Override
    public Optional<Security> getSecurityByTicker(String ticker) {
        Optional<Security> existing = securityRepository.findByTicker(ticker);
        if (existing.isPresent()) {
            return existing;
        }
        try {
            addSecurityWithTicker(ticker);
        } catch (MissingDataException e) {
            return Optional.empty();
        }
        return getSecurityByTicker(ticker);
    }

    @Override
    public Optional<Security> getSecurityById(Long securityId) {
        return securityRepository.findById(securityId);
    }

    @Override
    public List<Security> getAllByIdList(List<Long> currencyIds) {
        List<Security> securities = new ArrayList<>();
        securityRepository.findAllById(currencyIds).forEach(securities::add);
        return securities;
    }

    private Optional<SecurityPrice> getPriceFromExtendedRemoteSearch(String ticker, LocalDate from, LocalDate target) {
        Map<LocalDate, SecurityPrice> prices = remoteSecurityService.getPrices(ticker, from, target);
        List<SecurityPrice> newPrices = prices.values().stream()
                .filter(p -> priceRepository.countByTickerAndDate(ticker, p.getDate()) == 0).toList();
        priceRepository.saveAll( // NEW PRICES ARE ADDED AT THIS POINT
                newPrices.stream()
                        .map(SecurityPriceOfDay.class::cast)
                        .toList()
        );
        return Optional.ofNullable(prices.get(target));
    }

    private void verifyTickerIsPresent(String ticker) {
        if (securityRepository.countByTicker(ticker) == 0) {
            addSecurityWithTicker(ticker);
        }
    }

    private void addSecurityWithTicker(String ticker) {
        Optional<Security> security = remoteSecurityService.getSecurityDetails(ticker);
        if (security.isEmpty()) {
            throw new MissingDataException("No security found remotely with ticker: " + ticker);
        }
        securityRepository.save(security.get());
    }
}
