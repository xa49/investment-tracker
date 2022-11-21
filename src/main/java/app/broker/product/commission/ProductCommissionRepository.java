package app.broker.product.commission;

import app.util.DateRange;
import app.broker.HasCountById;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProductCommissionRepository extends JpaRepository<ProductCommission, Long>, HasCountById {

    @Query("SELECT NEW app.util.DateRange(c.id, c.fromDate, c.toDate) from ProductCommission c " +
            "WHERE c.product.id = :productId " +
            "AND c.market = :market " +
            "AND ((:fromDate IS NULL AND :toDate IS NULL) " +
            "OR (c.fromDate IS NULL AND (c.toDate IS NULL OR :fromDate IS NULL OR :fromDate <= c.toDate)) " +
            "OR (c.toDate IS NULL AND (:toDate >= c.fromDate OR :toDate IS NULL)) " +
            "OR ((:fromDate IS NULL OR :fromDate <= c.toDate) AND :toDate >= c.fromDate) " +
            "OR (:toDate IS NULL AND :fromDate <= c.toDate))")
    List<DateRange> getOverlappingDates(@Param("productId") Long productId,
                                        @Param("market") String market,
                                        @Param("fromDate") LocalDate fromDate,
                                        @Param("toDate") LocalDate toDate);

    @Query("SELECT NEW app.util.DateRange(c.id, c.fromDate, c.toDate) from ProductCommission c " +
            "WHERE c.product.id = :productId " +
            "AND c.id <> :excludedId " +
            "AND c.market = :market " +
            "AND ((:fromDate IS NULL AND :toDate IS NULL) " +
            "OR (c.fromDate IS NULL AND (c.toDate IS NULL OR :fromDate IS NULL OR :fromDate <= c.toDate)) " +
            "OR (c.toDate IS NULL AND (:toDate >= c.fromDate OR :toDate IS NULL)) " +
            "OR ((:fromDate IS NULL OR :fromDate <= c.toDate) AND :toDate >= c.fromDate) " +
            "OR (:toDate IS NULL AND :fromDate <= c.toDate))")
    List<DateRange> getOtherOverlappingDates(@Param("productId") Long productId,
                                             @Param("excludedId") Long excludedId,
                                             @Param("market") String market,
                                             @Param("fromDate") LocalDate fromDate,
                                             @Param("toDate") LocalDate toDate);


    int countById(Long id);

    int countByProductIdAndId(Long productId, Long id);

    @Query("SELECT c FROM ProductCommission c WHERE c.product.id = :productId " +
            "AND c.market = :market " +
            "AND ((c.fromDate IS NULL AND c.toDate IS NULL) " +
            "OR (c.fromDate IS NULL AND c.toDate >= :date) " +
            "OR (c.toDate IS NULL AND c.fromDate <= :date) " +
            "OR (c.fromDate <= :date AND c.toDate >= :date))")
    Optional<ProductCommission> getForProductAndMarketAndDate(@Param("productId") Long productId,
                                                              @Param("market") String market,
                                                              @Param("date") LocalDate date);

    List<ProductCommission> findAllByProductId(Long productId);
}
