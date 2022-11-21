package app.data.fx.mnb_access;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MNBQueryServiceIT {

    MNBQueryService mnbQueryService = new MNBQueryService();

    @Test
    void gettingKnownHistoricExchangeRates() {
        Map<LocalDate, BigDecimal> eurRates = mnbQueryService.getHufRates("EUR",
                LocalDate.of(2000, 1, 1), LocalDate.of(2000, 1, 7));
        assertEquals(4, eurRates.size());
        assertEquals(new BigDecimal("254.47"), eurRates.get(LocalDate.of(2000, 1, 4)));
        assertEquals(new BigDecimal("254.65"), eurRates.get(LocalDate.of(2000, 1, 7)));
    }

    @Test
    void gettingHufToHufRates() {
        Map<LocalDate, BigDecimal> rates = mnbQueryService.getHufRates("HUF",
                LocalDate.of(2000, 1, 1), LocalDate.of(2000, 1, 10));
        assertTrue(rates.isEmpty());
    }

    @Test
    void gettingKnownMissingRates() {
        LocalDate holidayWeekendStarts = LocalDate.of(2022,10,29);
        LocalDate holidayWeekendEnds = LocalDate.of(2022,11,1);

        Map<LocalDate, BigDecimal> rates = mnbQueryService.getHufRates("GBP", holidayWeekendStarts, holidayWeekendEnds);
        System.out.println(rates);
        assertTrue(rates.isEmpty());
    }

    @Test
    void gettingFutureRates() {
        Map<LocalDate, BigDecimal> eurRates = mnbQueryService.getHufRates("EUR",
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(20));
        assertTrue(eurRates.isEmpty());
    }

    @Test
    void shouldGetAllRatesInUnitsOfOne() {
        // JPY:HUF rates are commonly shown for 100 JPY
        Map<LocalDate, BigDecimal> jpyRates = mnbQueryService.getHufRates("JPY",
                LocalDate.of(2000, 1, 1), LocalDate.of(2000, 1, 9));
        assertEquals(4, jpyRates.size());
        assertEquals(new BigDecimal("2.4204"), jpyRates.get(LocalDate.of(2000, 1, 4)));
        assertEquals(new BigDecimal("2.3524"), jpyRates.get(LocalDate.of(2000, 1, 7)));
    }

    @Test
    void gettingInvalidCurrencyRates() {
        Map<LocalDate, BigDecimal> rates = mnbQueryService.getHufRates("WWW",
                LocalDate.of(2000, 1, 1), LocalDate.of(2000, 1, 9));
        assertTrue(rates.isEmpty());
    }

    @Test
    void gettingAValidCurrency() {
        MNBCurrency currency = mnbQueryService.getCurrencyDetails("KRW").orElseThrow();
        assertTrue(currency.isPresent());
        assertEquals("KRW", currency.getCurrency());
        assertEquals(100, currency.getUnits());
    }

    @Test
    void gettingInvalidCurrency() {
        Optional<MNBCurrency> currency = mnbQueryService.getCurrencyDetails("WWW");
        assertTrue(currency.isEmpty());
    }

}