package app.data.fx.rate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import javax.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ExchangeRateRepositoryTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    ExchangeRateRepository exchangeRateRepository;

    @Test
    void rateFound() {
        entityManager.persist(new StoredRate("EUR", "HUF", LocalDate.of(2000,10,10), BigDecimal.TEN));
        Optional<Rate> rate = exchangeRateRepository.findRate("EUR", "HUF", LocalDate.of(2000,10,10));

        assertTrue(rate.isPresent());
        assertEquals(BigDecimal.TEN, rate.get().getExchangeRate());
    }

    @Test
    void rateNotFound() {
        entityManager.persist(new StoredRate("EUR", "HUF", LocalDate.of(2000,10,10), BigDecimal.TEN));
        entityManager.persist(new StoredRate("JPY", "HUF", LocalDate.of(2000,10,9), BigDecimal.TEN));
        Optional<Rate> rate = exchangeRateRepository.findRate("JPY", "HUF", LocalDate.of(2000, 10, 10));
        assertTrue(rate.isEmpty());
    }

    @Test
    void isAlreadyStoredTrue() {
        entityManager.persist(new StoredRate("EUR", "HUF", LocalDate.of(2000,10,10), BigDecimal.TEN));
        assertTrue(exchangeRateRepository.isAlreadyStored("EUR", "HUF", LocalDate.of(2000,10,10)));
    }

    @Test
    void isAlreadyStoredFalse() {
        entityManager.persist(new StoredRate("EUR", "HUF", LocalDate.of(2000,10,10), BigDecimal.TEN));
        entityManager.persist(new StoredRate("JPY", "HUF", LocalDate.of(2000,10,9), BigDecimal.TEN));
        assertFalse(exchangeRateRepository.isAlreadyStored("JPY", "HUF", LocalDate.of(2000,10,10)));
    }
}