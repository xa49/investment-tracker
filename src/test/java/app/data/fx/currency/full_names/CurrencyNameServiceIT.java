package app.data.fx.currency.full_names;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


class CurrencyNameServiceIT {

    CurrencyNameService currencyNameService = new CurrencyNameService();

    @Test
    void validRequestAndResponse() {
        assertEquals("Euro", currencyNameService.getCurrencyName("EUR"));
        assertEquals("Japanese Yen", currencyNameService.getCurrencyName("JPY"));
        assertEquals("Hungarian Forint", currencyNameService.getCurrencyName("HUF"));
    }

    @Test
    void invalidCurrencyRequested() {
        IllegalArgumentException iae = assertThrows(IllegalArgumentException.class,
                () -> currencyNameService.getCurrencyName("WWW"));
        assertEquals("No full name found for currency: WWW", iae.getMessage());
    }
}