package app.manager.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    @Query("SELECT t FROM Transaction t LEFT JOIN FETCH t.takeFromAccount LEFT JOIN FETCH t.addToAccount " +
            "LEFT JOIN FETCH t.assetTaken LEFT JOIN FETCH t.assetAdded " +
            "WHERE (t.takeFromAccount.id IN :accountIds OR t.addToAccount.id IN :accountIds) " +
            "AND ((t.date >= :fromDate OR :fromDate IS NULL) AND (t.date <= :toDate OR :toDate IS NULL)) " +
            "ORDER BY t.date")
    List<Transaction> getTransactionsOnAccountsInPeriod(@Param("accountIds") List<Long> accountIds,
                                                        @Param("fromDate") LocalDate from,
                                                        @Param("toDate") LocalDate to);


    @Query("SELECT t FROM Transaction t LEFT JOIN FETCH t.addToAccount LEFT JOIN FETCH t.takeFromAccount " +
            "WHERE t.takeFromAccount.id = :accountId AND t.transactionType IN :transactionTypes")
    List<Transaction> getTransactionsByTypeAndTakeAccount(
            @Param("accountId") Long accountId, @Param("transactionTypes") List<TransactionType> transactionTypes);

}
