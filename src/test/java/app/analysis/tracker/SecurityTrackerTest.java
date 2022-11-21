package app.analysis.tracker;

import app.analysis.CashValue;
import app.analysis.actual.SecurityPosition;
import app.analysis.tracker.SecurityTracker;
import app.broker.account.BrokerAccount;
import app.broker.account.BrokerAccountType;
import app.data.securities.security.Security;
import app.manager.transaction.MatchingStrategy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SecurityTrackerTest {

    BrokerAccount mainAccount = createBrokerAccount("main", BrokerAccountType.MAIN);
    BrokerAccount tbszAccount = createBrokerAccount("tbsz", BrokerAccountType.TBSZ);
    Security security = new Security("ticker", "name", "Amsterdam", "EUR");
    Security differentSecurity = new Security("different", "different", "different", "EUR");
    SecurityPosition earlyEntryPos = new SecurityPosition(security, BigDecimal.TEN, new CashValue(new BigDecimal("4"), "EUR"),
            LocalDate.of(2000, 1, 1));
    SecurityPosition lateEntryPos = new SecurityPosition(security, new BigDecimal("7"), new CashValue(new BigDecimal("6"), "EUR"),
            LocalDate.of(2001, 2, 2));
    SecurityPosition differentSecurityPosition = new SecurityPosition(differentSecurity, new BigDecimal("5"), new CashValue(new BigDecimal("5"), "EUR"),
            LocalDate.of(2002, 1, 1));

    SecurityTracker securityTracker = new SecurityTracker();

    @Test
    void copyingReturnsADifferentInstanceWithSameState() {
        securityTracker.addPosition(mainAccount, earlyEntryPos);
        securityTracker.addPosition(mainAccount, lateEntryPos);
        securityTracker.addPosition(tbszAccount, differentSecurityPosition);

        SecurityTracker copied = securityTracker.copy();
        assertNotSame(securityTracker, copied);
        assertNotSame(securityTracker.getSecurityPositions(), copied.getSecurityPositions());
        assertEquals(securityTracker.getSecurityPositions(), copied.getSecurityPositions());
    }

    @Test
    void addingPositionToNewAccount() {
        securityTracker.addPosition(mainAccount, earlyEntryPos);
        assertEquals(1, securityTracker.getSecurityPositions().size());
        assertEquals(1, securityTracker.getSecurityPositions().get(mainAccount).size());
        assertEquals(List.of(earlyEntryPos), securityTracker.getSecurityPositions().get(mainAccount).get(security));
    }

    @Test
    void addingPositionToExistingAccountButNewSecurity() {
        securityTracker.addPosition(mainAccount, earlyEntryPos);
        securityTracker.addPosition(mainAccount, differentSecurityPosition);
        assertEquals(1, securityTracker.getSecurityPositions().size());
        assertEquals(2, securityTracker.getSecurityPositions().get(mainAccount).size());
        assertEquals(List.of(earlyEntryPos), securityTracker.getSecurityPositions().get(mainAccount).get(security));
        assertEquals(List.of(differentSecurityPosition), securityTracker.getSecurityPositions().get(mainAccount).get(differentSecurity));
    }

    @Test
    void addingPositionToExistingAccountAndSecurity() {
        securityTracker.addPosition(mainAccount, earlyEntryPos);
        securityTracker.addPosition(mainAccount, earlyEntryPos);
        assertEquals(1, securityTracker.getSecurityPositions().size());
        assertEquals(1, securityTracker.getSecurityPositions().get(mainAccount).size());
        assertEquals(List.of(earlyEntryPos, earlyEntryPos), securityTracker.getSecurityPositions().get(mainAccount).get(security));
    }

    @Test
    void closingFIFO_sufficientBalance() {
        securityTracker.addPosition(mainAccount, earlyEntryPos);
        securityTracker.addPosition(mainAccount, lateEntryPos);

        List<SecurityPosition> closedPositions = securityTracker.closePositions(mainAccount, security, new BigDecimal("15"), MatchingStrategy.FIFO);
        assertEquals(
                List.of(
                        new SecurityPosition(security, BigDecimal.TEN, earlyEntryPos.getEnteredAt(), earlyEntryPos.getEnterDate()),
                        new SecurityPosition(security, new BigDecimal("5"), lateEntryPos.getEnteredAt(), lateEntryPos.getEnterDate())),
                closedPositions);
        assertEquals(List.of(
                        new SecurityPosition(security, new BigDecimal("2"), lateEntryPos.getEnteredAt(), lateEntryPos.getEnterDate())),
                securityTracker.getSecurityPositions().get(mainAccount).get(security));

    }

    @Test
    void closingFIFO_insufficientBalance() {
        securityTracker.addPosition(mainAccount, earlyEntryPos);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> securityTracker.closePositions(mainAccount, security, new BigDecimal("15"), MatchingStrategy.FIFO));
        assertEquals("Could not find enough securities to sell: " + security.toString(), ex.getMessage());
    }

    @Test
    void closingLIFO_sufficientBalance() {
        securityTracker.addPosition(mainAccount, earlyEntryPos);
        securityTracker.addPosition(mainAccount, lateEntryPos);

        List<SecurityPosition> closedPositions = securityTracker.closePositions(mainAccount, security, new BigDecimal("15"), MatchingStrategy.LIFO);
        assertEquals(
                List.of(
                        new SecurityPosition(security, new BigDecimal("7"), lateEntryPos.getEnteredAt(), lateEntryPos.getEnterDate()),
                        new SecurityPosition(security, new BigDecimal("8"), earlyEntryPos.getEnteredAt(), earlyEntryPos.getEnterDate())),
                closedPositions);
        assertEquals(List.of(
                        new SecurityPosition(security, new BigDecimal("2"), earlyEntryPos.getEnteredAt(), earlyEntryPos.getEnterDate())),
                securityTracker.getSecurityPositions().get(mainAccount).get(security));
    }


    @Test
    void transferringToExistingAccount_withSameSecurity_FIFO() {
        securityTracker.addPosition(mainAccount, earlyEntryPos);
        securityTracker.addPosition(tbszAccount, earlyEntryPos);
        securityTracker.addPosition(tbszAccount, lateEntryPos);

        securityTracker.transferPositions(tbszAccount, mainAccount, security, new BigDecimal("11"), MatchingStrategy.FIFO);

        assertEquals(List.of(earlyEntryPos, earlyEntryPos, new SecurityPosition(security, BigDecimal.ONE, lateEntryPos.getEnteredAt(), lateEntryPos.getEnterDate())),
                securityTracker.getSecurityPositions().get(mainAccount).get(security));
        assertEquals(List.of(new SecurityPosition(security, new BigDecimal("6"), lateEntryPos.getEnteredAt(), lateEntryPos.getEnterDate())),
                securityTracker.getSecurityPositions().get(tbszAccount).get(security));
    }

    @Test
    void transferringToExistingAccount_withSameSecurity_LIFO() {
        securityTracker.addPosition(mainAccount, earlyEntryPos);
        securityTracker.addPosition(tbszAccount, earlyEntryPos);
        securityTracker.addPosition(tbszAccount, lateEntryPos);

        securityTracker.transferPositions(tbszAccount, mainAccount, security, new BigDecimal("11"), MatchingStrategy.LIFO);

        assertEquals(List.of(earlyEntryPos, lateEntryPos, new SecurityPosition(security, new BigDecimal("4"), earlyEntryPos.getEnteredAt(), earlyEntryPos.getEnterDate())),
                securityTracker.getSecurityPositions().get(mainAccount).get(security));
        assertEquals(List.of(new SecurityPosition(security, new BigDecimal("6"), earlyEntryPos.getEnteredAt(), earlyEntryPos.getEnterDate())),
                securityTracker.getSecurityPositions().get(tbszAccount).get(security));
    }

    @Test
    void transferringAllPositions() {
        securityTracker.addPosition(mainAccount, earlyEntryPos);
        securityTracker.addPosition(tbszAccount, earlyEntryPos);
        securityTracker.addPosition(tbszAccount, lateEntryPos);

        securityTracker.transferPositions(tbszAccount, mainAccount, security, new BigDecimal("17"), MatchingStrategy.LIFO);
        assertTrue(securityTracker.getSecurityPositions().get(tbszAccount).get(security).isEmpty());
    }

    @Test
    void transferringToExistingAccount_withoutSameSecurity() {
        securityTracker.addPosition(mainAccount, differentSecurityPosition);
        securityTracker.addPosition(tbszAccount, earlyEntryPos);
        securityTracker.addPosition(tbszAccount, lateEntryPos);

        securityTracker.transferPositions(tbszAccount, mainAccount, security, new BigDecimal("11"), MatchingStrategy.FIFO);

        assertEquals(List.of(earlyEntryPos, new SecurityPosition(security, BigDecimal.ONE, lateEntryPos.getEnteredAt(), lateEntryPos.getEnterDate())),
                securityTracker.getSecurityPositions().get(mainAccount).get(security));
        assertEquals(List.of(new SecurityPosition(security, new BigDecimal("6"), lateEntryPos.getEnteredAt(), lateEntryPos.getEnterDate())),
                securityTracker.getSecurityPositions().get(tbszAccount).get(security));
    }

    @Test
    void transferringToExistingAccount_insufficientBalance() {
        securityTracker.addPosition(mainAccount, differentSecurityPosition);
        securityTracker.addPosition(tbszAccount, earlyEntryPos);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> securityTracker.transferPositions(tbszAccount, mainAccount, security, new BigDecimal("11"), MatchingStrategy.FIFO));
        assertEquals("Could not find enough securities to sell: " + security.toString(), ex.getMessage());
    }

    @Test
    void transferringToNewAccount() {
        securityTracker.addPosition(tbszAccount, earlyEntryPos);
        securityTracker.addPosition(tbszAccount, lateEntryPos);

        securityTracker.transferPositions(tbszAccount, mainAccount, security, new BigDecimal("11"), MatchingStrategy.FIFO);

        assertEquals(List.of(earlyEntryPos, new SecurityPosition(security, BigDecimal.ONE, lateEntryPos.getEnteredAt(), lateEntryPos.getEnterDate())),
                securityTracker.getSecurityPositions().get(mainAccount).get(security));
        assertEquals(List.of(new SecurityPosition(security, new BigDecimal("6"), lateEntryPos.getEnteredAt(), lateEntryPos.getEnterDate())),
                securityTracker.getSecurityPositions().get(tbszAccount).get(security));
    }

    @Test
    void mergingNeitherExists() {
        securityTracker.mergeAccounts(tbszAccount, mainAccount);
        assertTrue(securityTracker.getSecurityPositions().isEmpty());

    }

    @Test
    void mergingTargetDoesNotExist() {
        securityTracker.addPosition(tbszAccount, lateEntryPos);

        securityTracker.mergeAccounts(tbszAccount, mainAccount);
        assertEquals(Map.of(mainAccount, Map.of(security, List.of(lateEntryPos))), securityTracker.getSecurityPositions());
    }

    @Test
    void mergingTargetExists_withoutSecurity() {
        securityTracker.addPosition(mainAccount, differentSecurityPosition);
        securityTracker.addPosition(tbszAccount, lateEntryPos);

        securityTracker.mergeAccounts(tbszAccount, mainAccount);
        assertEquals(Map.of(mainAccount, Map.of(security, List.of(lateEntryPos), differentSecurity, List.of(differentSecurityPosition))),
                securityTracker.getSecurityPositions());
    }

    @Test
    void mergingTargetExists_withSecurity() {
        securityTracker.addPosition(mainAccount, earlyEntryPos);
        securityTracker.addPosition(tbszAccount, lateEntryPos);

        securityTracker.mergeAccounts(tbszAccount, mainAccount);
        assertEquals(Map.of(mainAccount, Map.of(security, List.of(earlyEntryPos, lateEntryPos))),
                securityTracker.getSecurityPositions());
    }

    @Test
    void mergingAccountWithItself() {
        securityTracker.addPosition(mainAccount, earlyEntryPos);

        securityTracker.mergeAccounts(mainAccount, mainAccount);
        assertEquals(Map.of(mainAccount, Map.of(security, List.of(earlyEntryPos))),
                securityTracker.getSecurityPositions());
    }

    private BrokerAccount createBrokerAccount(String name, BrokerAccountType type) {
        BrokerAccount account = new BrokerAccount();
        account.setName(name);
        account.setAccountType(type);
        return account;
    }

}