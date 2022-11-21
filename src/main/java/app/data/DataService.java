package app.data;

import app.data.fx.currency.BasicCurrency;
import app.data.fx.currency.CurrencyService;
import app.data.fx.rate.ExchangeRateService;
import app.data.fx.rate.Rate;
import app.data.securities.SecurityService;
import app.data.securities.price.SecurityPrice;
import app.data.securities.security.Security;
import app.util.InvalidDataException;
import app.util.MissingDataException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * <p>DataService has the responsibility to collect asset pricing data for its clients from multiple
 * alternative sources.</p>
 * <p>Methods in this class either returns the requested value or throws an exception. The decision about which exception to
 * throw happens here.</p>
 * <p>All own methods serving DataService have Optional return values.</p>
 */

@Service
@AllArgsConstructor
public class DataService {

    private static final int SHARE_PRICE_DATE_OFFSET_TOLERANCE = 7;
    private static final int EXCHANGE_DATE_OFFSET_TOLERANCE = 7;
    private final ExchangeRateService exchangeRateService;
    private final SecurityService securityService;
    private final CurrencyService currencyService;

    // Exchange rates
    public BigDecimal getExchangeRate(String sourceCurrency, String destinationCurrency, LocalDate exchangeDate) {
        Optional<Rate> exchangeRateDetails = exchangeRateService
                .getExchangeRateDetails(sourceCurrency, destinationCurrency, exchangeDate, EXCHANGE_DATE_OFFSET_TOLERANCE);
        if (exchangeRateDetails.isPresent()) {
            return exchangeRateDetails.get().getExchangeRate();
        }
        throw new MissingDataException("No exchange rate is available for " + sourceCurrency + ":"
                + destinationCurrency + " in the " + EXCHANGE_DATE_OFFSET_TOLERANCE + " day period to " + exchangeDate);
    }

    public BasicCurrency getCurrencyDetailsByCode(String currencyCode) {
        return currencyService.getByCode(currencyCode)
                .orElseThrow(() -> new InvalidDataException("Currency not found with code: " + currencyCode));
    }

    public BasicCurrency getCurrencyDetailsById(Long currencyId) {
        return currencyService.getById(currencyId)
                .orElseThrow(() -> new InvalidDataException("Currency not found with database id: " + currencyId));
    }

    public List<BasicCurrency> getCurrencyDetailsByIdList(List<Long> currencyIds) {
        List<BasicCurrency> foundEntries = currencyService.getAllById(currencyIds);
        if (foundEntries.size() == currencyIds.size()) {
            return foundEntries;
        }
        throw new InvalidDataException("Could not find currencies with database id(s): "
                + new ArrayList<>(currencyIds).removeAll(foundEntries.stream().map(BasicCurrency::getId).toList()));
    }

    // Securities
    public SecurityPrice getSharePrice(String ticker, LocalDate priceDate) {
        return securityService.getPrice(ticker, priceDate, SHARE_PRICE_DATE_OFFSET_TOLERANCE)
                .orElseThrow(() -> new MissingDataException("Share price not found for " + ticker + " in the "
                        + SHARE_PRICE_DATE_OFFSET_TOLERANCE + " day period to " + priceDate));
    }

    public Security getSecurityDetailsById(Long securityId) {
        return securityService.getSecurityById(securityId)
                .orElseThrow(() -> new InvalidDataException("No security found with database id: " + securityId));
    }

    public Security getSecurityDetailsByTicker(String ticker) {
        return securityService.getSecurityByTicker(ticker)
                .orElseThrow(() -> new InvalidDataException("No security found with ticker: " + ticker));
    }

    public String getSecurityMarketByTicker(String ticker) {
        return securityService.getMarketByTicker(ticker)
                .orElseThrow(() -> new MissingDataException(("No market found for security with ticker: " + ticker)));
    }

    public List<Security> getSecurityDetailsByIdList(List<Long> currencyIds) {
        List<Security> foundEntries = securityService.getAllByIdList(currencyIds);
        if (foundEntries.size() == currencyIds.size()) {
            return foundEntries;
        }
        throw new InvalidDataException("Could not find securities with database id(s): "
                + new ArrayList<>(currencyIds).removeAll(foundEntries.stream().map(Security::getId).toList()));
    }
}