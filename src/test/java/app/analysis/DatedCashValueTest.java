package app.analysis;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DatedCashValueTest {

    @Test
    void equalsAndHashCode() {
        DatedCashValue first = new DatedCashValue(BigDecimal.ONE, "HUF", LocalDate.EPOCH);
        DatedCashValue second = new DatedCashValue(BigDecimal.ONE, "HUF", LocalDate.EPOCH);
        DatedCashValue differentAmount = new DatedCashValue(null, "HUF", LocalDate.EPOCH);
        DatedCashValue differentCurrency = new DatedCashValue(BigDecimal.ONE, null, LocalDate.EPOCH);
        DatedCashValue differentDate = new DatedCashValue(BigDecimal.ONE, "HUF", null);

        assertTrue(first.equals(second) && second.equals(first));
        assertTrue(!first.equals(differentAmount) && !first.equals(differentCurrency) && !first.equals(differentDate));
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void semiCopyConstructor() {
        assertEquals(BigDecimal.TEN,
                new DatedCashValue(new CashValue(BigDecimal.TEN, "HUF"), LocalDate.EPOCH).getAmount());
    }

}