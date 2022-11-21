package app.analysis.tracker;

import app.analysis.CashValue;
import app.analysis.DatedCashValue;
import app.broker.account.BrokerAccount;
import app.broker.account.BrokerAccountType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class CashTrackerTest {

    BrokerAccount mainAccount = createBrokerAccount("main", BrokerAccountType.MAIN);
    BrokerAccount tbszAccount = createBrokerAccount("tbsz", BrokerAccountType.TBSZ);

    CashTracker cashTracker = new CashTracker();

    @Test
    void copyingReturnsACompletelyDifferentObject() {
        cashTracker.addReturnCashFlow(LocalDate.of(2000, 1, 1), "GBP", BigDecimal.TEN);
        cashTracker.addBrokerCashBalance(mainAccount, "USD", BigDecimal.ONE);

        CashTracker copy = cashTracker.copy();


        assertNotSame(cashTracker, copy);
        assertNotSame(cashTracker.getBrokerBalances(), copy.getBrokerBalances());
        assertNotSame(cashTracker.getReturnCashFlows(), copy.getReturnCashFlows());

        // DatedInvestorCashFlows also different (two loops to handle if list order changed)
        for (DatedCashValue inOriginal : cashTracker.getReturnCashFlows()) {
            for (DatedCashValue inCopy : copy.getReturnCashFlows()) {
                assertNotSame(inOriginal, inCopy);
            }
        }

        assertEquals(cashTracker.getBrokerBalances(), copy.getBrokerBalances());
        assertEquals(cashTracker.getReturnCashFlows(), copy.getReturnCashFlows());
    }

    @Test
    void copyingALargeObject() {
        // This method normally takes <40ms for 2 BrokerAccounts with 1_000 currencies each and a 100_000-element cash flow list
        cashTracker.addReturnCashFlow(LocalDate.of(2000, 1, 1), "GBP", BigDecimal.TEN);
        IntStream.range(1, 100000).forEach(
                i -> cashTracker.addBrokerCashBalance(
                        i % 2 == 0 ? mainAccount : tbszAccount,
                        "USD" + (i % 999),
                        BigDecimal.ONE
                ));

        CashTracker copy = cashTracker.copy();


        assertNotSame(cashTracker, copy);
        assertNotSame(cashTracker.getBrokerBalances(), copy.getBrokerBalances());
        assertNotSame(cashTracker.getReturnCashFlows(), copy.getReturnCashFlows());

    }

    @Test
    void addingCashBalance() {
        cashTracker.addBrokerCashBalance(mainAccount, "EUR", BigDecimal.ONE);
        assertEquals(BigDecimal.ONE, cashTracker.getBrokerBalances().get(mainAccount).get("EUR"));

        cashTracker.addBrokerCashBalance(mainAccount, "EUR", BigDecimal.ONE);
        assertEquals(new BigDecimal("2"), cashTracker.getBrokerBalances().get(mainAccount).get("EUR"));

        cashTracker.addBrokerCashBalance(mainAccount, "USD", BigDecimal.TEN);
        assertEquals(2, cashTracker.getBrokerBalances().get(mainAccount).keySet().size());

        // Return cash flows are not updated automatically
        assertEquals(Collections.emptyList(), cashTracker.getReturnCashFlows());
    }

    @Test
    void deductCashBalance() {
        cashTracker.addBrokerCashBalance(mainAccount, "EUR", BigDecimal.ONE);
        assertEquals(BigDecimal.ONE, cashTracker.getBrokerBalances().get(mainAccount).get("EUR"));
        cashTracker.deductBrokerBalance(mainAccount, "EUR", BigDecimal.ONE);
        assertEquals(BigDecimal.ZERO, cashTracker.getBrokerBalances().get(mainAccount).get("EUR"));

        assertEquals(Collections.emptyList(), cashTracker.getReturnCashFlows());
    }

    @Test
    void addingCashFlow_onlyHasNoEffectOnBrokerBalances() {
        cashTracker.addReturnCashFlow(LocalDate.of(2000, 1, 1), "EUR", BigDecimal.ONE);
        assertTrue(cashTracker.getBrokerBalances().isEmpty());
    }

    @Test
    void addingCashFlow_showsInBalance() {
        cashTracker.addReturnCashFlow(LocalDate.EPOCH, "EUR", BigDecimal.ONE);
        cashTracker.addReturnCashFlow(DatedCashValue.of(LocalDate.EPOCH, BigDecimal.TEN, "GBP"));
        assertEquals(List.of(DatedCashValue.of(LocalDate.EPOCH, BigDecimal.ONE, "EUR"),
                        DatedCashValue.of(LocalDate.EPOCH, BigDecimal.TEN, "GBP")),
                cashTracker.getReturnCashFlows());
    }

    @Test
    void addingNewBrokerAccountCreatesNewEntry() {
        cashTracker.addBrokerCashBalance(mainAccount, "EUR", BigDecimal.ONE);
        cashTracker.addBrokerCashBalance(tbszAccount, "EUR", BigDecimal.ONE);
        assertEquals(2, cashTracker.getBrokerBalances().size());
    }

    @Test
    void mergingAccountWithItself_hasNoEffect() {
        cashTracker.addBrokerCashBalance(mainAccount, "EUR", BigDecimal.ONE);
        cashTracker.mergeAccounts(mainAccount, mainAccount);
        assertEquals(1, cashTracker.getBrokerBalances().size());
        assertEquals(BigDecimal.ONE, cashTracker.getBrokerBalances().get(mainAccount).get("EUR"));
    }

    @Test
    void mergingAccounts_addingNewCurrencyToTarget() {
        cashTracker.addBrokerCashBalance(mainAccount, "EUR", BigDecimal.ONE);
        cashTracker.addBrokerCashBalance(tbszAccount, "EUR", BigDecimal.ONE);
        cashTracker.addBrokerCashBalance(tbszAccount, "GBP", BigDecimal.ONE);
        cashTracker.mergeAccounts(tbszAccount, mainAccount);
        assertEquals(1, cashTracker.getBrokerBalances().size());
        assertEquals(Set.of("EUR", "GBP"), cashTracker.getBrokerBalances().get(mainAccount).keySet());
        assertEquals(new BigDecimal(2), cashTracker.getBrokerBalances().get(mainAccount).get("EUR"));
        assertEquals(new BigDecimal(1), cashTracker.getBrokerBalances().get(mainAccount).get("GBP"));
    }

    @Test
    void mergingAccounts_whenTargetDoesNotExist() {
        cashTracker.addBrokerCashBalance(tbszAccount, "EUR", BigDecimal.ONE);
        cashTracker.mergeAccounts(tbszAccount, mainAccount);
        assertEquals(Set.of(mainAccount), cashTracker.getBrokerBalances().keySet());
        assertEquals(BigDecimal.ONE, cashTracker.getBrokerBalances().get(mainAccount).get("EUR"));
    }

    @Test
    void mergingAccounts_whenSourceDoesNotExist() {
        cashTracker.addBrokerCashBalance(mainAccount, "EUR", BigDecimal.ONE);
        cashTracker.mergeAccounts(tbszAccount, mainAccount);
        assertEquals(Set.of(mainAccount), cashTracker.getBrokerBalances().keySet());
        assertEquals(BigDecimal.ONE, cashTracker.getBrokerBalances().get(mainAccount).get("EUR"));
    }

    @Test
    void movingCashToBank() {
        cashTracker.addBrokerCashBalance(mainAccount, "EUR", BigDecimal.ONE);
        cashTracker.addBrokerCashBalance(tbszAccount, "GBP", BigDecimal.ONE);
        cashTracker.addBrokerCashBalance(tbszAccount, "EUR", BigDecimal.ONE);

        cashTracker.moveBrokerCashToBank();
        assertEquals(Map.of("EUR", new BigDecimal(2), "GBP", BigDecimal.ONE),
                cashTracker.getBankBalances());
        assertEquals(Collections.emptyMap(), cashTracker.getBrokerBalances());
    }

    @Test
    void deductingFromBankBalance_couldGoIntoNegative() {
        cashTracker.deductBankBalance(CashValue.of(BigDecimal.ONE, "EUR"));
        assertEquals(Map.of("EUR", BigDecimal.ONE.negate()), cashTracker.getBankBalances());
    }

    private BrokerAccount createBrokerAccount(String name, BrokerAccountType type) {
        BrokerAccount account = new BrokerAccount();
        account.setName(name);
        account.setAccountType(type);
        return account;
    }

}