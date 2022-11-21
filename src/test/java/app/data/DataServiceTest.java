package app.data;

import app.data.fx.currency.BasicCurrency;
import app.data.fx.currency.CurrencyService;
import app.data.fx.rate.ExchangeRateService;
import app.data.fx.rate.StoredRate;
import app.data.securities.SecurityService;
import app.data.securities.price.SecurityPrice;
import app.data.securities.price.SecurityPriceOfDay;
import app.data.securities.security.Security;
import app.util.InvalidDataException;
import app.util.MissingDataException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataServiceTest {

    @Mock
    ExchangeRateService exchangeRateService;

    @Mock
    SecurityService securityService;

    @Mock
    CurrencyService currencyService;

    @InjectMocks
    DataService dataService;

    @Test
    void exchangeRateFound() {
        when(exchangeRateService.getExchangeRateDetails(eq("KWD"), eq("JPY"), eq(LocalDate.of(2000, 1, 1)), anyInt()))
                .thenReturn(Optional.of(new StoredRate("KWD", "JPY", LocalDate.of(1999, 12, 31), BigDecimal.TEN)));

        BigDecimal exchangeRate = dataService.getExchangeRate("KWD", "JPY", LocalDate.of(2000, 1, 1));
        assertEquals(BigDecimal.TEN, exchangeRate);
    }

    @Test
    void exchangeRateNotFound() {
        when(exchangeRateService.getExchangeRateDetails(eq("KWD"), eq("JPY"), eq(LocalDate.of(2000, 1, 1)), anyInt()))
                .thenReturn(Optional.empty());

        MissingDataException ex = assertThrows(MissingDataException.class,
                () -> dataService.getExchangeRate("KWD", "JPY", LocalDate.of(2000, 1, 1)));
        assertThat(ex.getMessage(), matchesPattern("No exchange rate is available for KWD:JPY in the [0-9]+ day period to 2000-01-01"));
    }

    @Test
    void currencyDetailsFoundByCode() {
        when(currencyService.getByCode("JPY"))
                .thenReturn(Optional.of(new BasicCurrency("JPY", "Japanese Yen")));

        assertEquals("Japanese Yen", dataService.getCurrencyDetailsByCode("JPY").getFullName());
    }

    @Test
    void currencyDetailsNotFoundByCode() {
        when(currencyService.getByCode("WWW"))
                .thenReturn(Optional.empty());

        InvalidDataException ex = assertThrows(InvalidDataException.class,
                () -> dataService.getCurrencyDetailsByCode("WWW"));
        assertEquals("Currency not found with code: WWW", ex.getMessage());
    }

    @Test
    void currencyDetailsFoundById() {
        when(currencyService.getById(5L))
                .thenReturn(Optional.of(new BasicCurrency("JPY", "Japanese Yen")));

        assertEquals("Japanese Yen", dataService.getCurrencyDetailsById(5L).getFullName());
    }

    @Test
    void currencyDetailsNotFoundById() {
        when(currencyService.getById(5L))
                .thenReturn(Optional.empty());

        InvalidDataException ex = assertThrows(InvalidDataException.class,
                () -> dataService.getCurrencyDetailsById(5L));
        assertEquals("Currency not found with database id: 5", ex.getMessage());

    }

    @Test
    void sharePriceFound() {
        when(securityService.getPrice(eq("MMM"), eq(LocalDate.of(2000, 1, 1)), anyInt()))
                .thenReturn(Optional.of(SecurityPriceOfDay.builder().date(LocalDate.of(1999, 12, 31)).close(BigDecimal.TEN).build()));

        SecurityPrice price = dataService.getSharePrice("MMM", LocalDate.of(2000, 1, 1));

        assertEquals(BigDecimal.TEN, price.getPrice());
        assertEquals(LocalDate.of(1999, 12, 31), price.getDate());
    }

    @Test
    void sharePriceNotFound() {
        when(securityService.getPrice(eq("MMM"), eq(LocalDate.of(2000, 1, 1)), anyInt()))
                .thenReturn(Optional.empty());

        MissingDataException ex = assertThrows(MissingDataException.class,
                () -> dataService.getSharePrice("MMM", LocalDate.of(2000, 1, 1)));
        assertThat(ex.getMessage(), matchesPattern("Share price not found for MMM in the [0-9]+ day period to 2000-01-01"));
    }

    @Test
    void securityDetailsFoundById() {
        when(securityService.getSecurityById(5L))
                .thenReturn(Optional.of(new Security("MMM", "3M Corporation", "NYSE", "USD")));

        assertEquals("3M Corporation", dataService.getSecurityDetailsById(5L).getFullName());
    }

    @Test
    void securityDetailsNotFoundById() {
        when(securityService.getSecurityById(5L))
                .thenReturn(Optional.empty());

        InvalidDataException ex = assertThrows(InvalidDataException.class,
                () -> dataService.getSecurityDetailsById(5L));
        assertEquals("No security found with database id: 5", ex.getMessage());
    }

    @Test
    void securityDetailsFoundByTicker() {
        when(securityService.getSecurityByTicker("MMM"))
                .thenReturn(Optional.of(new Security("MMM", "3M Corporation", "NYSE", "USD")));

        assertEquals("3M Corporation", dataService.getSecurityDetailsByTicker("MMM").getFullName());
    }

    @Test
    void securityDetailsNotFoundByTicker() {
        when(securityService.getSecurityByTicker("QQQ111"))
                .thenReturn(Optional.empty());

        InvalidDataException ex = assertThrows(InvalidDataException.class,
                () -> dataService.getSecurityDetailsByTicker("QQQ111"));
        assertEquals("No security found with ticker: QQQ111", ex.getMessage());
    }

    @Test
    void marketFoundByTicker() {
        when(securityService.getMarketByTicker("MMM"))
                .thenReturn(Optional.of("NYSE"));

        assertEquals("NYSE", dataService.getSecurityMarketByTicker("MMM"));
    }

    @Test
    void marketNotFoundByTicker() {
        when(securityService.getMarketByTicker("QQQ111"))
                .thenReturn(Optional.empty());

        MissingDataException ex = assertThrows(MissingDataException.class,
                () -> dataService.getSecurityMarketByTicker("QQQ111"));
        assertEquals("No market found for security with ticker: QQQ111", ex.getMessage());
    }
}
