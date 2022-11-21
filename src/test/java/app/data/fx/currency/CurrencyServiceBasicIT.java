package app.data.fx.currency;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Sql(scripts = {"classpath:/cleanbroker.sql"})
class CurrencyServiceBasicIT {

    @Autowired
    CurrencyServiceBasic currencyService;

    @Test
    void gettingCurrencyFromMNBAndFullNameService() {
        // Database is emptied
        BasicCurrency currency = currencyService.getByCode("CZK").orElseThrow();
        assertEquals("Czech Republic Koruna", currency.getFullName());
    }

    @Test
    void gettingInvalidCurrencyFromMNB() {
        Optional<BasicCurrency> currency = currencyService.getByCode("WWW");
        assertTrue(currency.isEmpty());
    }

}