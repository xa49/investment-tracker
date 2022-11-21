package app.broker.product;

import app.util.DateRange;
import app.broker.HasCountById;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BrokerProductRepository extends JpaRepository<BrokerProduct, Long>, HasCountById {
    @Query("SELECT NEW app.util.DateRange(p.id, p.fromDate, p.toDate) from BrokerProduct p " +
            "WHERE p.broker.id = :brokerId " +
            "AND p.name = :name " +
            "AND ((:fromDate IS NULL AND :toDate IS NULL) " +
            "OR (p.fromDate IS NULL AND (p.toDate IS NULL OR :fromDate IS NULL OR :fromDate <= p.toDate)) " +
            "OR (p.toDate IS NULL AND (:toDate >= p.fromDate OR :toDate IS NULL)) " +
            "OR ((:fromDate IS NULL OR :fromDate <= p.toDate) AND :toDate >= p.fromDate) " +
            "OR (:toDate IS NULL AND :fromDate <= p.toDate))")
    List<DateRange> getOverlappingDates(@Param("brokerId") Long brokerId,
                                        @Param("name") String name,
                                        @Param("fromDate") LocalDate fromDate,
                                        @Param("toDate") LocalDate toDate);

    @Query("SELECT NEW app.util.DateRange(p.id, p.fromDate, p.toDate) from BrokerProduct p " +
            "WHERE p.broker.id = :brokerId " +
            "AND p.id <> :excludedId " +
            "AND p.name = :name " +
            "AND ((:fromDate IS NULL AND :toDate IS NULL) " +
            "OR (p.fromDate IS NULL AND (p.toDate IS NULL OR :fromDate IS NULL OR :fromDate <= p.toDate)) " +
            "OR (p.toDate IS NULL AND (:toDate >= p.fromDate OR :toDate IS NULL)) " +
            "OR ((:fromDate IS NULL OR :fromDate <= p.toDate) AND :toDate >= p.fromDate) " +
            "OR (:toDate IS NULL AND :fromDate <= p.toDate))")
    List<DateRange> getOtherOverlappingDates(@Param("brokerId") Long brokerId,
                                             @Param("excludedId") Long excludedId,
                                             @Param("name") String name,
                                             @Param("fromDate") LocalDate fromDate,
                                             @Param("toDate") LocalDate toDate);
    @Query("SELECT p FROM BrokerProduct p WHERE p.broker.id = :brokerId AND p.name = :name " +
            "AND ((p.fromDate IS NULL AND p.toDate IS NULL) " +
            "OR (p.fromDate IS NULL AND p.toDate >= :date) " +
            "OR (p.toDate IS NULL AND p.fromDate <= :date) " +
            "OR (p.fromDate <= :date AND p.toDate >= :date))")
    Optional<BrokerProduct> getByNameAndDate(
            @Param("brokerId") Long brokerId, @Param("name") String name, @Param("date") LocalDate date);

    int countById(Long id);

    int countByBrokerIdAndId(Long brokerId, Long id);

    List<BrokerProduct> findAllByBrokerId(Long brokerId);
}
