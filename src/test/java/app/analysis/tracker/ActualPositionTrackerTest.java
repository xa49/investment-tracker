package app.analysis.tracker;

import app.analysis.CashValue;
import app.analysis.DatedCashValue;
import app.analysis.actual.SecurityPosition;
import app.analysis.actual.TaxEffectDto;
import app.broker.account.BrokerAccount;
import app.data.securities.security.Security;
import app.manager.transaction.MatchingStrategy;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActualPositionTrackerTest {

    @Spy
    CashTracker cashTracker;

    @Spy
    SecurityTracker securityTracker;

    @Spy
    TaxTracker taxTracker;

    @InjectMocks
    ActualPositionTracker actualPositionTracker;

    @Test
    void getBlankInstance_comesWithInitialisedTrackers() {
        ActualPositionTracker basic = ActualPositionTracker.getBlank();
        assertNotNull(basic.taxTracker);
        assertNotNull(basic.cashTracker);
        assertNotNull(basic.securityTracker);
    }

    @Test
    void getBlankInstance_alwaysReturnsNewOne() {
        ActualPositionTracker basic = ActualPositionTracker.getBlank();
        ActualPositionTracker another = ActualPositionTracker.getBlank();
        assertNotSame(basic, another);
    }

    @Test
    void getBrokerCashBalances_returnUnmodifiableCopy() {
        Map<BrokerAccount, Map<String, BigDecimal>> balances = actualPositionTracker.getBrokerCashBalances();
        assertNotSame(cashTracker.getBrokerBalances(), balances);
        assertThrows(UnsupportedOperationException.class, () -> balances.put(new BrokerAccount(), Collections.emptyMap()));
    }

    @Test
    void getSecurityPositions_returnUnmodifiableCopy() {
        Map<BrokerAccount, Map<Security, List<SecurityPosition>>> positions = actualPositionTracker.getSecurityPositions();
        assertNotSame(securityTracker.getSecurityPositions(), positions);
        assertThrows(UnsupportedOperationException.class, () -> positions.put(new BrokerAccount(), Collections.emptyMap()));

    }

    @Test
    void getReturnCashFlows_returnsUnmodifiableCopy() {
        List<DatedCashValue> cashFlows = actualPositionTracker.getReturnCashFlows();
        assertNotSame(cashTracker.getReturnCashFlows(), cashFlows);
        assertThrows(UnsupportedOperationException.class, () -> cashFlows.add(new DatedCashValue(BigDecimal.ZERO, "EUR", LocalDate.now())));
    }

    @Test
    void processMoneyIn_showsInBrokerBalanceAndCashFlow() {
        BrokerAccount account = new BrokerAccount();
        actualPositionTracker.processMoneyIn(LocalDate.of(2000, 1, 1), account, "GBP", BigDecimal.TEN);

        assertEquals(BigDecimal.TEN, actualPositionTracker.getBrokerCashBalances().get(account).get("GBP"));
        assertEquals(List.of(new DatedCashValue(BigDecimal.TEN.negate(), "GBP", LocalDate.of(2000, 1, 1))),
                actualPositionTracker.getReturnCashFlows());
    }

    @Test
    void processMoneyOut_showsInBrokerBalanceAndCashFlow() {
        BrokerAccount account = new BrokerAccount();
        actualPositionTracker.processMoneyOut(LocalDate.of(2000, 1, 1), account, "GBP", BigDecimal.TEN);

        assertEquals(BigDecimal.TEN.negate(), actualPositionTracker.getBrokerCashBalances().get(account).get("GBP"));
        assertEquals(List.of(new DatedCashValue(BigDecimal.TEN, "GBP", LocalDate.of(2000, 1, 1))),
                actualPositionTracker.getReturnCashFlows());
    }

    @Test
    void processEnterInvestment_showsInSecurityAndCashBalances_notShowsInReturnCashFlow() {
        BrokerAccount account = new BrokerAccount();
        Security security = new Security("T", "security", "NYSE", "USD");
        actualPositionTracker.processEnterInvestment(LocalDate.of(2000, 1, 1), account, security, BigDecimal.TEN,
                "EUR", BigDecimal.ONE);

        assertEquals(BigDecimal.ONE.negate(), actualPositionTracker.getBrokerCashBalances().get(account).get("EUR"));
        assertEquals(
                List.of(new SecurityPosition(security, BigDecimal.TEN,
                        CashValue.of(new BigDecimal("0.1"), "EUR"), LocalDate.of(2000, 1, 1))),
                actualPositionTracker.getSecurityPositions().get(account).get(security));
        assertEquals(Collections.emptyList(), actualPositionTracker.getReturnCashFlows());
    }

    @Test
    void processExitInvestment_showsInSecurityAndCashBalances_notShowsInReturnCashFlow() {
        BrokerAccount account = new BrokerAccount();
        Security security = new Security("T", "security", "NYSE", "USD");

        actualPositionTracker.processEnterInvestment(LocalDate.of(2000, 1, 1), account, security, BigDecimal.TEN,
                "EUR", BigDecimal.ONE);

        actualPositionTracker.processExitInvestment(account, security, new BigDecimal("9"),
                CashValue.of(BigDecimal.TEN, "EUR"), MatchingStrategy.FIFO);

        assertEquals(Map.of(account, Map.of("EUR", new BigDecimal("9"))), actualPositionTracker.getBrokerCashBalances());
        assertEquals(Collections.emptyList(), actualPositionTracker.getReturnCashFlows());
        assertEquals(
                Map.of(account,
                        Map.of(security, List.of(
                                new SecurityPosition(security, BigDecimal.ONE, CashValue.of(new BigDecimal("0.1"), "EUR"), LocalDate.of(2000, 1, 1))))),
                actualPositionTracker.getSecurityPositions());
    }

    @Test
    void processExitInvestmentTax_doesNothingForZeros() {
        TaxEffectDto empty = TaxEffectDto.builder()
                .taxPaid(CashValue.of(BigDecimal.ZERO, "HUF"))
                .lossAdded(CashValue.of(BigDecimal.ZERO, "HUF"))
                .lossesUsed(Collections.emptyList())
                .feeUsed(CashValue.of(BigDecimal.ZERO, "HUF"))
                .build();

        actualPositionTracker.changeTaxCurrency("HUF", BigDecimal.ONE);
        actualPositionTracker.processTax(empty);

        assertEquals(Collections.emptyList(), actualPositionTracker.getLossOffsetAvailable());
        assertEquals(new CashValue(BigDecimal.ZERO, "HUF"), actualPositionTracker.getFeeWriteOffAvailable());
        verify(taxTracker, never()).addTaxPayment(any());
        verify(taxTracker, never()).addLossOffset(any());
        verify(taxTracker, never()).removeUsedUpLosses(any());
        verify(taxTracker, never()).useFeeWriteOff(any());
    }

    @Test
    void processExitInvestmentTax_handlesNonZeros() {
        TaxEffectDto partial = TaxEffectDto.builder()
                .taxPaid(CashValue.of(BigDecimal.ONE, "HUF"))
                .lossAdded(CashValue.of(BigDecimal.ZERO, "HUF"))
                .lossesUsed(Collections.emptyList())
                .feeUsed(CashValue.of(BigDecimal.ONE, "HUF"))
                .transactionDate(LocalDate.EPOCH)
                .build();

        taxTracker.changeTaxationCurrency("HUF", BigDecimal.ONE);
        taxTracker.addAvailableFeeWriteOff(CashValue.of(BigDecimal.TEN, "HUF"));
        // Set up balances to not go into negative

        actualPositionTracker.processTax(partial);

        assertEquals(Collections.emptyList(), actualPositionTracker.getLossOffsetAvailable());
        assertEquals(CashValue.of(new BigDecimal("9"), "HUF"), actualPositionTracker.getFeeWriteOffAvailable());
        verify(taxTracker).addTaxPayment(DatedCashValue.of(LocalDate.EPOCH, BigDecimal.ONE, "HUF"));
        verify(taxTracker, never()).addLossOffset(any());
        verify(taxTracker, never()).removeUsedUpLosses(any());
        verify(taxTracker).useFeeWriteOff(CashValue.of(BigDecimal.ONE, "HUF"));
    }

    @Test
    void processExitInvestmentTax_handlesAll() {
        TaxEffectDto full = TaxEffectDto.builder()
                .taxPaid(CashValue.of(BigDecimal.ONE, "HUF"))
                .lossAdded(CashValue.of(BigDecimal.ONE, "HUF"))
                .lossesUsed(List.of(DatedCashValue.of(LocalDate.EPOCH, BigDecimal.TEN, "HUF")))
                .feeUsed(CashValue.of(BigDecimal.ONE, "HUF"))
                .transactionDate(LocalDate.EPOCH.plusDays(1))
                .build();

        actualPositionTracker.changeTaxCurrency("HUF", BigDecimal.ONE);
        taxTracker.addLossOffset(DatedCashValue.of(LocalDate.EPOCH, BigDecimal.TEN, "HUF")); // to have enough to remove
        taxTracker.addAvailableFeeWriteOff(CashValue.of(BigDecimal.TEN, "HUF"));
        actualPositionTracker.changeTaxCurrency("HUF", BigDecimal.ONE);
        actualPositionTracker.processTax(full);

        assertEquals(List.of(DatedCashValue.of(LocalDate.EPOCH.plusDays(1), BigDecimal.ONE, "HUF")), actualPositionTracker.getLossOffsetAvailable());
        assertEquals(CashValue.of(new BigDecimal("9"), "HUF"), actualPositionTracker.getFeeWriteOffAvailable());
        verify(taxTracker).addTaxPayment(DatedCashValue.of(LocalDate.EPOCH.plusDays(1), BigDecimal.ONE, "HUF"));
        verify(taxTracker).addLossOffset(DatedCashValue.of(LocalDate.EPOCH.plusDays(1), BigDecimal.ONE, "HUF"));
        verify(taxTracker).removeUsedUpLosses(List.of(DatedCashValue.of(LocalDate.EPOCH, BigDecimal.TEN, "HUF")));
        verify(taxTracker).useFeeWriteOff(CashValue.of(BigDecimal.ONE, "HUF"));
    }

    @Test
    void processTransferSecurity_showsInSecurityBalance() {
        BrokerAccount fromAccount = new BrokerAccount();
        fromAccount.setName("from");
        BrokerAccount toAccount = new BrokerAccount();
        toAccount.setName("to");
        Security security = new Security("T", "security", "NYSE", "USD");

        actualPositionTracker.processEnterInvestment(LocalDate.of(2000, 1, 1), fromAccount, security, BigDecimal.TEN,
                "EUR", BigDecimal.ONE);

        actualPositionTracker.processTransferSecurity(fromAccount, toAccount, security, new BigDecimal("9"), MatchingStrategy.FIFO);
        // ONE left in source account
        assertEquals(List.of(new SecurityPosition(security, BigDecimal.ONE, CashValue.of(new BigDecimal("0.1"), "EUR"), LocalDate.of(2000, 1, 1))),
                actualPositionTracker.getSecurityPositions().get(fromAccount).get(security));
        // 9 added to destination account
        assertEquals(List.of(new SecurityPosition(security, new BigDecimal("9"), CashValue.of(new BigDecimal("0.1"), "EUR"), LocalDate.of(2000, 1, 1))),
                actualPositionTracker.getSecurityPositions().get(toAccount).get(security));
    }

    @Test
    void processTransferCash_showsInCashBalances_notInCashFlow() {
        BrokerAccount fromAccount = new BrokerAccount();
        fromAccount.setName("from");
        BrokerAccount toAccount = new BrokerAccount();
        toAccount.setName("to");

        actualPositionTracker.processTransferCash(fromAccount, toAccount, "EUR", BigDecimal.TEN);
        assertEquals(BigDecimal.TEN.negate(), actualPositionTracker.getBrokerCashBalances().get(fromAccount).get("EUR"));
        assertEquals(BigDecimal.TEN, actualPositionTracker.getBrokerCashBalances().get(toAccount).get("EUR"));
        assertEquals(Collections.emptyList(), actualPositionTracker.getReturnCashFlows());
    }

    @Test
    void processPayFee_showsInCashBalance_addsAvailableFeeWriteOff_notShowsInReturnCashFlow() {
        actualPositionTracker.changeTaxCurrency("HUF", BigDecimal.ONE);
        BrokerAccount account = new BrokerAccount();
        actualPositionTracker.processFee(account, "EUR", BigDecimal.TEN,
                CashValue.of(BigDecimal.ONE, "HUF"));

        assertEquals(BigDecimal.TEN.negate(), actualPositionTracker.getBrokerCashBalances().get(account).get("EUR"));
        assertEquals(Collections.emptyList(), actualPositionTracker.getReturnCashFlows());
        assertEquals(CashValue.of(BigDecimal.ONE, "HUF"), actualPositionTracker.getFeeWriteOffAvailable());
    }

    @Test
    void changeTaxCurrency_changesCurrencyAndAmounts() {
        assertNull(actualPositionTracker.getTaxationCurrency());
        actualPositionTracker.changeTaxCurrency("JPY", BigDecimal.ONE);
        assertEquals("JPY", actualPositionTracker.getTaxationCurrency());

        taxTracker.addAvailableFeeWriteOff(CashValue.of(BigDecimal.TEN, "JPY"));
        taxTracker.addLossOffset(DatedCashValue.of(LocalDate.EPOCH, BigDecimal.ONE, "JPY"));
        actualPositionTracker.changeTaxCurrency("GBP", new BigDecimal("2"));
        assertEquals(CashValue.of(new BigDecimal("20"), "GBP"), actualPositionTracker.getFeeWriteOffAvailable());
        assertEquals(List.of(DatedCashValue.of(LocalDate.EPOCH, new BigDecimal("2"), "GBP")), actualPositionTracker.getLossOffsetAvailable());
    }

    @Test
    void getTaxationCurrency() {
        actualPositionTracker.changeTaxCurrency("HUF", BigDecimal.ONE);
        actualPositionTracker.getTaxationCurrency();
        assertEquals("HUF", actualPositionTracker.getTaxationCurrency());
    }

    @Test
    void getFeeWriteOffAvailable() {
        when(taxTracker.getFeeWriteOffAvailable())
                .thenReturn(new CashValue(BigDecimal.TEN, "EUR"));

        assertEquals(new CashValue(BigDecimal.TEN, "EUR"), actualPositionTracker.getFeeWriteOffAvailable());
    }

    @Test
    void getLossOffsetAvailable() {
        when(taxTracker.getLossOffsetAvailable())
                .thenReturn(List.of(
                        new DatedCashValue(BigDecimal.TEN, "EUR", LocalDate.EPOCH)
                ));

        assertEquals(List.of(new DatedCashValue(BigDecimal.TEN, "EUR", LocalDate.EPOCH)),
                actualPositionTracker.getLossOffsetAvailable());
    }
}