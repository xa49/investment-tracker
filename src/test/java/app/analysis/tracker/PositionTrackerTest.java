package app.analysis.tracker;

import app.analysis.CashValue;
import app.analysis.DatedCashValue;
import app.broker.account.BrokerAccount;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PositionTrackerTest {

    static class PurePositionTracker extends PositionTracker {
        public PurePositionTracker(CashTracker cashTracker, SecurityTracker securityTracker, TaxTracker taxTracker) {
            super(cashTracker, securityTracker, taxTracker);
        }
    }

    @Spy
    CashTracker cashTracker;

    @Spy
    SecurityTracker securityTracker;

    @Spy
    TaxTracker taxTracker;

    @InjectMocks
    PurePositionTracker tracker;

    @Test
    void getBrokerCashBalances() {
        tracker.getBrokerCashBalances();
        verify(cashTracker).getBrokerBalances();
    }

    @Test
    void getReturnCashFlows() {
        tracker.getReturnCashFlows();
        verify(cashTracker).getReturnCashFlows();
    }

    @Test
    void getSecurityPositions() {
        tracker.getSecurityPositions();
        verify(securityTracker).getSecurityPositions();
    }

    @Test
    void getFeeWriteOffAvailable() {
        doReturn(new CashValue(BigDecimal.TEN, "EUR"))
                .when(taxTracker).getFeeWriteOffAvailable();
        assertEquals(new CashValue(BigDecimal.TEN, "EUR"), tracker.getFeeWriteOffAvailable());
    }

    @Test
    void getLossOffsetAvailable() {
        assertEquals(Collections.emptyList(), tracker.getLossOffsetAvailable());
        verify(taxTracker).getLossOffsetAvailable();
    }

    @Test
    void addLossOffset() {
        taxTracker.changeTaxationCurrency("EUR", BigDecimal.ONE);
        taxTracker.addLossOffset(new DatedCashValue(BigDecimal.TEN, "EUR", LocalDate.EPOCH));
        assertEquals(List.of(new DatedCashValue(BigDecimal.TEN, "EUR", LocalDate.EPOCH)),
                tracker.getLossOffsetAvailable());
    }

    @Test
    void removeUsedUpLosses() {
        taxTracker.changeTaxationCurrency("EUR", BigDecimal.ONE);
        taxTracker.addLossOffset(new DatedCashValue(BigDecimal.TEN, "EUR", LocalDate.EPOCH));

        taxTracker.removeUsedUpLosses(List.of(new DatedCashValue(new BigDecimal("9"), "EUR", LocalDate.EPOCH)));
        assertEquals(List.of(new DatedCashValue(BigDecimal.ONE, "EUR", LocalDate.EPOCH)), tracker.getLossOffsetAvailable());
    }

    @Test
    void getTaxationCurrency() {
        doReturn("HUF")
                .when(taxTracker).getTaxationCurrency();
        assertEquals("HUF", tracker.getTaxationCurrency());
    }

    @Test
    void changeTaxCurrency() {
        tracker.changeTaxCurrency("GBP", BigDecimal.ONE);
        assertEquals("GBP", tracker.getTaxationCurrency());

        tracker.taxTracker.addAvailableFeeWriteOff(new CashValue(BigDecimal.ONE, "GBP"));
        tracker.changeTaxCurrency("JPY", BigDecimal.TEN);
        assertEquals(new CashValue(BigDecimal.TEN, "JPY"), tracker.getFeeWriteOffAvailable());
    }

    @Test
    void processFee_deductsFromBrokerBalance_addsFeeWriteOff() {
        tracker.changeTaxCurrency("HUF", BigDecimal.ONE);
        tracker.processFee(new BrokerAccount(), "EUR", BigDecimal.ONE, CashValue.of(BigDecimal.TEN, "HUF"));
        assertEquals(Map.of(new BrokerAccount(), Map.of("EUR", BigDecimal.ONE.negate())), tracker.getBrokerCashBalances());
        assertEquals(CashValue.of(BigDecimal.TEN, "HUF"), tracker.getFeeWriteOffAvailable());
    }
}