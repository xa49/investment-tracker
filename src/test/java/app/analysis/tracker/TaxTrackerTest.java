package app.analysis.tracker;

import app.analysis.DatedCashValue;
import app.analysis.CashValue;
import app.util.InvalidDataException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TaxTrackerTest {

    TaxTracker taxTracker = TaxTracker.getBlank();

    @BeforeEach
    void init() {
        taxTracker.changeTaxationCurrency("HUF", BigDecimal.ONE);
    }

    @Test
    void copyConstructorReturnsIndependentObject() {
        taxTracker.addLossOffset(new DatedCashValue(BigDecimal.ONE, "HUF", LocalDate.of(2000, 1, 1)));
        taxTracker.addAvailableFeeWriteOff(new CashValue(BigDecimal.ONE, "HUF"));
        taxTracker.addUntaxedGain(new CashValue(BigDecimal.ONE, "HUF"));

        TaxTracker copy = new TaxTracker(taxTracker);

        assertNotSame(taxTracker, copy);
        assertNotSame(taxTracker.getFeeWriteOffAvailable(), copy.getFeeWriteOffAvailable());
        assertNotSame(taxTracker.getLossOffsetAvailable(), copy.getLossOffsetAvailable());
        assertNotSame(taxTracker.getUntaxedGain(), copy.getUntaxedGain());

        for (DatedCashValue originalCashFlow : taxTracker.getLossOffsetAvailable()) {
            for (DatedCashValue copyCashFlow : copy.getLossOffsetAvailable()) {
                assertNotSame(originalCashFlow, copyCashFlow);
            }
        }

        assertEquals(taxTracker.getLossOffsetAvailable(), copy.getLossOffsetAvailable());
        assertEquals(taxTracker.getUntaxedGain(), copy.getUntaxedGain());
        assertEquals(taxTracker.getFeeWriteOffAvailable(), copy.getFeeWriteOffAvailable());
        assertEquals(taxTracker.getTaxationCurrency(), copy.getTaxationCurrency());
    }

    @Test
    void addLossOffsetCorrectCurrency() {
        taxTracker.addLossOffset(new DatedCashValue(BigDecimal.ONE, "HUF", LocalDate.of(2000, 1, 1)));
        assertEquals(1, taxTracker.getLossOffsetAvailable().size());
        assertEquals(BigDecimal.ONE, taxTracker.getLossOffsetAvailable().get(0).getAmount());
    }

    @Test
    void addFeeCorrectCurrency_asDatedInvestorCashFlow() {
        taxTracker.addAvailableFeeWriteOff(new DatedCashValue(BigDecimal.ONE, "HUF", LocalDate.of(2000, 1, 1)));
        assertEquals(new CashValue(BigDecimal.ONE, "HUF"), taxTracker.getFeeWriteOffAvailable());
    }

    @Test
    void addUntaxedGainCorrectCurrency() {
        taxTracker.addUntaxedGain(new CashValue(BigDecimal.ONE, "HUF"));
        assertEquals(new CashValue(BigDecimal.ONE, "HUF"), taxTracker.getUntaxedGain());
    }

    @Test
    void addLossOffsetIncorrectCurrency() {
        InvalidDataException ex = assertThrows(InvalidDataException.class,
                () -> taxTracker.addLossOffset(new DatedCashValue(BigDecimal.ONE, "EUR", LocalDate.of(2000, 1, 1))));
        assertEquals("TaxTracker received EUR update request but it's taxation currency is: HUF", ex.getMessage());
    }

    @Test
    void addFeeCorrectIncorrectCurrency() {
        InvalidDataException ex = assertThrows(InvalidDataException.class,
                () -> taxTracker.addAvailableFeeWriteOff(new DatedCashValue(BigDecimal.ONE, "EUR", LocalDate.of(2000, 1, 1))));
        assertEquals("TaxTracker received EUR update request but it's taxation currency is: HUF", ex.getMessage());
    }

    @Test
    void addUntaxedGainIncorrectCurrency() {
        InvalidDataException ex = assertThrows(InvalidDataException.class,
                () -> taxTracker.addUntaxedGain(new CashValue(BigDecimal.ONE, "EUR")));
        assertEquals("TaxTracker received EUR update request but it's taxation currency is: HUF", ex.getMessage());
    }

    @Test
    void changingTaxationCurrency() {
        taxTracker.addLossOffset(new DatedCashValue(BigDecimal.ONE, "HUF", LocalDate.of(2000, 1, 1)));
        taxTracker.addAvailableFeeWriteOff(new CashValue(BigDecimal.ONE, "HUF"));
        taxTracker.addUntaxedGain(new CashValue(BigDecimal.ONE, "HUF"));

        taxTracker.changeTaxationCurrency("EUR", BigDecimal.TEN);
        assertEquals(new CashValue(BigDecimal.TEN, "EUR"), taxTracker.getFeeWriteOffAvailable());
        assertEquals(new CashValue(BigDecimal.TEN, "EUR"), taxTracker.getUntaxedGain());
        assertEquals(new DatedCashValue(BigDecimal.TEN, "EUR", LocalDate.of(2000, 1, 1)), taxTracker.getLossOffsetAvailable().get(0));
    }

    @Test
    void changingTaxationCurrencyToNull() {
        InvalidDataException ex = assertThrows(InvalidDataException.class,
                () -> taxTracker.changeTaxationCurrency(null, BigDecimal.ONE));
        assertEquals("Cannot set taxation currency to null.", ex.getMessage());
    }

    @Test
    void changingTaxationCurrencyWithInvalidData() {
        InvalidDataException ex = assertThrows(InvalidDataException.class,
                () -> taxTracker.changeTaxationCurrency("EUR", null));
        assertEquals("Exchange rate to new currency must be positive.", ex.getMessage());

        ex = assertThrows(InvalidDataException.class,
                () -> taxTracker.changeTaxationCurrency("EUR", BigDecimal.ZERO));
        assertEquals("Exchange rate to new currency must be positive.", ex.getMessage());
    }

    @Test
    void useFeeWriteOff_enoughBalance() {
        taxTracker.addAvailableFeeWriteOff(new CashValue(BigDecimal.TEN, "HUF"));
        taxTracker.useFeeWriteOff(new CashValue(BigDecimal.ONE, "HUF"));
        assertEquals(new CashValue(new BigDecimal("9"), "HUF"), taxTracker.getFeeWriteOffAvailable());
    }

    @Test
    void useFeeWriteOff_notEnoughBalance() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () ->         taxTracker.useFeeWriteOff(new CashValue(BigDecimal.ONE, "HUF")));
        assertEquals("Not enough fee write off to deduct.", ex.getMessage());
    }

    @Test
    void useFeeWriteOff_illegalCurrency() {
        taxTracker.addAvailableFeeWriteOff(new CashValue(BigDecimal.TEN, "HUF"));
        InvalidDataException ex = assertThrows(InvalidDataException.class,
                () ->taxTracker.useFeeWriteOff(new CashValue(BigDecimal.ONE, "EUR")));
        assertEquals("TaxTracker received EUR update request but it's taxation currency is: HUF", ex.getMessage());
    }

    @Test
    void addingAndQueryingTaxPayment() {
        taxTracker.addTaxPayment(DatedCashValue.of(LocalDate.EPOCH, BigDecimal.ONE, "EUR"));
        assertEquals(List.of(DatedCashValue.of(LocalDate.EPOCH, BigDecimal.ONE, "EUR")),
                taxTracker.getTaxesPaid());
    }

    @Test
    void removeLossOffsetsNotAvailableAnymore() {
        taxTracker.addLossOffset(new DatedCashValue(BigDecimal.TEN, "HUF", LocalDate.of(2000, 1, 1)));
        taxTracker.addLossOffset(new DatedCashValue(BigDecimal.TEN, "HUF", LocalDate.of(2000, 1, 2)));
        taxTracker.addLossOffset(new DatedCashValue(BigDecimal.TEN, "HUF", LocalDate.of(2000, 1, 3)));

        taxTracker.removeUsedUpLosses(List.of(
                new DatedCashValue(BigDecimal.TEN, "HUF", LocalDate.of(2000, 1, 1)),
                new DatedCashValue(new BigDecimal("4"), "HUF", LocalDate.of(2000, 1, 2))
        ));

        assertEquals(2, taxTracker.getLossOffsetAvailable().size());
        assertFalse(taxTracker.getLossOffsetAvailable().stream().anyMatch(cf -> cf.getDate().equals(LocalDate.of(2000, 1, 1))));
        assertEquals(new BigDecimal("16"), taxTracker.getLossOffsetAvailable().stream().map(DatedCashValue::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    @Test
    void removeLossOffsetDoesNotLeaveZeroValues() {
        taxTracker.addLossOffset(new DatedCashValue(BigDecimal.TEN, "HUF", LocalDate.of(2000, 1, 1)));
        taxTracker.removeUsedUpLosses(List.of(
                new DatedCashValue(BigDecimal.ONE, "HUF", LocalDate.of(2000, 1, 1)),
                new DatedCashValue(new BigDecimal("9"), "HUF", LocalDate.of(2000, 1, 1))
        ));

        assertTrue(taxTracker.getLossOffsetAvailable().isEmpty());
    }

    @Test
    void removingMoreThanAvailable() {
        taxTracker.addLossOffset(new DatedCashValue(BigDecimal.TEN, "HUF", LocalDate.of(2000, 1, 1)));

        List<DatedCashValue> excessiveList = List.of(
                new DatedCashValue(new BigDecimal("15"), "HUF", LocalDate.of(2000, 1, 1))
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> taxTracker.removeUsedUpLosses(excessiveList));
        assertEquals("Removed too much or too little loss offset. Remaining to remove: 5", ex.getMessage());
    }


}