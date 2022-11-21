package app.analysis;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CashValueTest {

    @Test
    void testEqualsAndHashCode() {
        CashValue first = new CashValue(BigDecimal.TEN, "HUF");
        CashValue second = new CashValue(BigDecimal.TEN, "HUF");
        CashValue differentAmount = new CashValue(BigDecimal.ONE, "HUF");
        CashValue differentCurrency = new CashValue(BigDecimal.TEN, "EUR");

        assertTrue(first.equals(second) && second.equals(first));
        assertEquals(first.hashCode(), second.hashCode());
        assertTrue(!first.equals(differentAmount) && !first.equals(differentCurrency));
        assertNotEquals(first.hashCode(), differentAmount.hashCode());
    }


    @Test
    void getAmount() {
        assertEquals(BigDecimal.ONE, new CashValue(BigDecimal.ONE, "HUF").getAmount());
    }

    @Test
    void getCurrency() {
        assertEquals("HUF", new CashValue(BigDecimal.ONE, "HUF").getCurrency());
    }
}