package app.data.securities.yahoo_access;

import app.data.securities.price.SecurityPrice;
import app.data.securities.security.Security;
import app.util.MissingDataException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class YahooRemoteSecurityServiceIT {

    YahooRemoteSecurityService yahooRemoteSecurityService = new YahooRemoteSecurityService();

    @Test
    void gettingPricesForValidTicker() {
        Map<LocalDate, SecurityPrice> prices = yahooRemoteSecurityService.getPrices("MMM", LocalDate.of(2010, 1, 1), LocalDate.of(2010, 1, 5));
        assertTrue(prices.containsKey(LocalDate.of(2010,1,4)));
        assertTrue(prices.containsKey(LocalDate.of(2010,1,5)));
        assertEquals(82.5, prices.get(LocalDate.of(2010,1,5)).getPrice().doubleValue());
    }

    @Test
    void gettingPricesForInvalidTicker() {
        MissingDataException ex = assertThrows(MissingDataException.class,
                () -> yahooRemoteSecurityService.getPrices("QQ222XXXYYYTTTZZZ", LocalDate.of(2000, 1, 1), LocalDate.of(2000, 1, 2)));
        assertEquals("Could not get stock prices from Yahoo for QQ222XXXYYYTTTZZZ from 2000-01-01 to 2000-01-02", ex.getMessage());
    }

    @Test
    void gettingSecurityDetailsForValidTicker() {
        Optional<Security> securityDetails = yahooRemoteSecurityService.getSecurityDetails("MMM");
        assertTrue(securityDetails.isPresent());
        assertEquals("3M Company", securityDetails.get().getFullName());
        assertEquals("NYSE", securityDetails.get().getMarket());
    }

    @Test
    void gettingSecurityDetailsForInvalidTicker() {
        Optional<Security> invalidSecurity = yahooRemoteSecurityService.getSecurityDetails("QQ222XXXYYYTTTZZZ");
        assertTrue(invalidSecurity.isEmpty());
    }

}