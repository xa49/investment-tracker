package app.manager.transaction;

import app.broker.account.BrokerAccount;
import app.manager.transaction.asset_record.InvestmentAssetRecord;
import app.manager.transaction.asset_record.InvestmentAssetType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import javax.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TransactionRepositoryTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionRepository transactionRepository;

    BrokerAccount account1 = new BrokerAccount();
    BrokerAccount account2 = new BrokerAccount();

    InvestmentAssetRecord security = new InvestmentAssetRecord(InvestmentAssetType.SECURITY, 5L);
    InvestmentAssetRecord cash = new InvestmentAssetRecord(InvestmentAssetType.CASH, 6L);

    @BeforeEach
    void init() {
        entityManager.persist(account1);
        entityManager.persist(account2);

        entityManager.persist(security);
        entityManager.persist(cash);

        // Five transactions
        Transaction takeAccount2_moneyIn_2000Jan01 =
                Transaction.builder(LocalDate.of(2000, 1, 1), TransactionType.MONEY_IN)
                        .take(BigDecimal.TEN, cash, account2)
                        .build();
        Transaction takeAccount2_enterInvestment_2000Jan03 =
                Transaction.builder(LocalDate.of(2000, 1, 3), TransactionType.ENTER_INVESTMENT)
                        .take(BigDecimal.TEN, security, account2)
                        .build();
        Transaction takeAccount1_enterInvestment_2000Jan03 =
                Transaction.builder(LocalDate.of(2000, 1, 3), TransactionType.ENTER_INVESTMENT)
                        .take(BigDecimal.TEN, security, account1)
                        .build();
        Transaction takeAccount2MoneyOut2000Jan1 =
                Transaction.builder(LocalDate.of(2000, 1, 1), TransactionType.MONEY_OUT)
                        .take(BigDecimal.TEN, cash, account2)
                        .build();
        Transaction noTakeAccount_moneyIn_2000Jan02 =
                Transaction.builder(LocalDate.of(2000, 1, 2), TransactionType.MONEY_IN)
                        .add(BigDecimal.TEN, cash, account2)
                        .build();

        entityManager.persist(takeAccount2_moneyIn_2000Jan01);
        entityManager.persist(takeAccount2_enterInvestment_2000Jan03);
        entityManager.persist(takeAccount1_enterInvestment_2000Jan03);
        entityManager.persist(takeAccount2MoneyOut2000Jan1);
        entityManager.persist(noTakeAccount_moneyIn_2000Jan02);
    }

    @Test
    void gettingTransactionsOnAccountsInPeriod() {
        List<Transaction> transactions = transactionRepository.getTransactionsOnAccountsInPeriod(
                List.of(account1.getId(), account2.getId()),
                LocalDate.of(2000, 1, 1), LocalDate.of(2000, 1, 3));
        assertEquals(5, transactions.size());
        assertEquals(Set.of(TransactionType.MONEY_IN, TransactionType.ENTER_INVESTMENT, TransactionType.MONEY_OUT),
                transactions.stream().map(Transaction::getTransactionType).collect(Collectors.toSet()));
    }

    @Test
    void gettingTransactionsOnAccountsInPeriod_undefinedStart() {
        List<Transaction> transactions = transactionRepository.getTransactionsOnAccountsInPeriod(List.of(account2.getId()),
                null, LocalDate.of(2000, 1, 2));
        assertEquals(3, transactions.size());
        assertFalse(transactions.stream().anyMatch(t -> t.getDate().isAfter(LocalDate.of(2000, 1, 2))));
    }

    @Test
    void gettingTransactionsOnAccountsInPeriod_undefinedEnd() {
        List<Transaction> transactions = transactionRepository.getTransactionsOnAccountsInPeriod(List.of(account2.getId()),
                LocalDate.of(2000, 1, 2), null);
        assertEquals(2, transactions.size());
        assertFalse(transactions.stream().anyMatch(t -> t.getDate().isBefore(LocalDate.of(2000, 1, 2))));
    }

    @Test
    void gettingTransactionsOnAccountsInPeriod_undefinedInterval() {
        List<Transaction> transactions = transactionRepository.getTransactionsOnAccountsInPeriod(List.of(account1.getId(), account2.getId()),
                null, null);
        assertEquals(5, transactions.size());
    }

    @Test
    void gettingTransactionsByTypeOnTakeAccount() {
        List<Transaction> transactions = transactionRepository.getTransactionsByTypeAndTakeAccount(account2.getId(),
                List.of(TransactionType.MONEY_IN, TransactionType.ENTER_INVESTMENT));
        assertEquals(2, transactions.size());
        assertEquals(Set.of(TransactionType.MONEY_IN, TransactionType.ENTER_INVESTMENT),
                transactions.stream().map(Transaction::getTransactionType).collect(Collectors.toSet()));
    }
}