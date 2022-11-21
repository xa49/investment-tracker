package app.broker.account.association;

import app.util.DateRange;
import app.broker.HasCountById;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProductAssociationRepository extends JpaRepository<ProductAssociation, Long> , HasCountById {

    int countById(Long id);

    @Query("SELECT NEW app.util.DateRange(a.id, a.fromDate, a.toDate) from ProductAssociation a " +
            "WHERE a.account.id = :accountId " +
            "AND a.product.id = :productId " +
            "AND ((:fromDate IS NULL AND :toDate IS NULL) " +
            "OR (a.fromDate IS NULL AND (a.toDate IS NULL OR :fromDate IS NULL OR :fromDate <= a.toDate)) " +
            "OR (a.toDate IS NULL AND (:toDate >= a.fromDate OR :toDate IS NULL)) " +
            "OR ((:fromDate IS NULL OR :fromDate <= a.toDate) AND :toDate >= a.fromDate) " +
            "OR (:toDate IS NULL AND :fromDate <= a.toDate))")
    List<DateRange> getOverlappingDates(@Param("accountId") Long accountId,
                                        @Param("productId") Long productId,
                                        @Param("fromDate") LocalDate fromDate,
                                        @Param("toDate") LocalDate toDate);

    @Query("SELECT NEW app.util.DateRange(a.id, a.fromDate, a.toDate) from ProductAssociation a " +
            "WHERE a.account.id = :accountId " +
            "AND a.product.id = :productId " +
            "AND NOT a.id = :id " +
            "AND ((:fromDate IS NULL AND :toDate IS NULL) " +
            "OR (a.fromDate IS NULL AND (a.toDate IS NULL OR :fromDate IS NULL OR :fromDate <= a.toDate)) " +
            "OR (a.toDate IS NULL AND (:toDate >= a.fromDate OR :toDate IS NULL)) " +
            "OR ((:fromDate IS NULL OR :fromDate <= a.toDate) AND :toDate >= a.fromDate) " +
            "OR (:toDate IS NULL AND :fromDate <= a.toDate))")
    List<DateRange> getOtherOverlappingDates(@Param("accountId") Long accountId, @Param("productId") Long productId,
                                             @Param("id") Long associationId, @Param("fromDate") LocalDate fromDate,
                                             @Param("toDate") LocalDate toDate);

    @Query("SELECT a FROM ProductAssociation a LEFT JOIN FETCH a.product p LEFT JOIN FETCH p.broker " +
            "WHERE a.account.id = :accountId " +
            "AND ((a.fromDate IS NULL OR a.fromDate <= :date) AND (a.toDate IS NULL OR a.toDate >= :date))")
    Optional<ProductAssociation> getAssociationOnDay(@Param("accountId") Long accountId, @Param("date") LocalDate date);

    List<ProductAssociation> findAllByAccountId(Long productId);

    @Query("SELECT a FROM ProductAssociation a LEFT JOIN FETCH a.product p LEFT JOIN FETCH p.broker " +
            "WHERE a.account.id = :accountId ORDER BY a.fromDate")
    List<ProductAssociation> getAccountHistory(@Param("accountId") Long accountId);

    @Query("SELECT a FROM ProductAssociation a LEFT JOIN FETCH a.product p LEFT JOIN FETCH p.broker b LEFT JOIN FETCH a.account n " +
            "WHERE n.accountType = 'MAIN' AND b.id = :brokerId " +
            "AND ((a.fromDate IS NULL OR a.fromDate <= :date) AND (a.toDate IS NULL OR a.toDate >= :date))")
    Optional<ProductAssociation> getMainAccountAssociation(@Param("brokerId") Long brokerId, @Param("date") LocalDate date);
}
