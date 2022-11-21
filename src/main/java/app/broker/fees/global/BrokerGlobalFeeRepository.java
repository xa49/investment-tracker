package app.broker.fees.global;

import app.util.DateRange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface BrokerGlobalFeeRepository extends JpaRepository<BrokerGlobalFee, Long> {

    @Query("SELECT NEW app.util.DateRange(f.id, f.fromDate, f.toDate) from BrokerGlobalFee f " +
            "WHERE f.broker.id = :brokerId " +
            "AND ((:fromDate IS NULL AND :toDate IS NULL) " +
            "OR (f.fromDate IS NULL AND (f.toDate IS NULL OR :fromDate IS NULL OR :fromDate <= f.toDate)) " +
            "OR (f.toDate IS NULL AND (:toDate >= f.fromDate OR :toDate IS NULL)) " +
            "OR ((:fromDate IS NULL OR :fromDate <= f.toDate) AND :toDate >= f.fromDate) " +
            "OR (:toDate IS NULL AND :fromDate <= f.toDate))")
    List<DateRange> getOverlappingDates(@Param("brokerId") Long brokerId,
                                        @Param("fromDate") LocalDate fromDate,
                                        @Param("toDate") LocalDate toDate);

    @Query("SELECT NEW app.util.DateRange(f.id, f.fromDate, f.toDate) from BrokerGlobalFee f " +
            "WHERE f.broker.id = :brokerId " +
            "AND f.id <> :excludedId " +
            "AND ((:fromDate IS NULL AND :toDate IS NULL) " +
            "OR (f.fromDate IS NULL AND (f.toDate IS NULL OR :fromDate IS NULL OR :fromDate <= f.toDate)) " +
            "OR (f.toDate IS NULL AND (:toDate >= f.fromDate OR :toDate IS NULL)) " +
            "OR ((:fromDate IS NULL OR :fromDate <= f.toDate) AND :toDate >= f.fromDate) " +
            "OR (:toDate IS NULL AND :fromDate <= f.toDate))")
    List<DateRange> getOtherOverlappingDates(@Param("brokerId") Long brokerId,
                                             @Param("excludedId") Long excludedId,
                                             @Param("fromDate") LocalDate fromDate,
                                             @Param("toDate") LocalDate toDate);

    @Query("SELECT f FROM BrokerGlobalFee f WHERE f.broker.id = :brokerId " +
            "AND ((f.fromDate IS NULL AND f.toDate IS NULL) " +
            "OR (f.fromDate IS NULL AND f.toDate >= :day) " +
            "OR (f.toDate IS NULL AND f.fromDate <= :day) " +
            "OR (f.fromDate <= :day AND f.toDate >= :day))")
    Optional<BrokerGlobalFee> getGlobalFeeForBrokerAndDay(@Param("brokerId") Long brokerId, @Param("day") LocalDate day);

    int countByBrokerIdAndId(Long brokerId, Long id);

    @Query("SELECT f FROM BrokerGlobalFee f WHERE f.broker.id IN :brokerIds " +
            "AND ((:fromDate IS NULL AND :toDate IS NULL) " +
            "OR (f.fromDate IS NULL AND (f.toDate IS NULL OR :fromDate IS NULL OR :fromDate <= f.toDate)) " +
            "OR (f.toDate IS NULL AND (:toDate >= f.fromDate OR :toDate IS NULL)) " +
            "OR ((:fromDate IS NULL OR :fromDate <= f.toDate) AND :toDate >= f.fromDate) " +
            "OR (:toDate IS NULL AND :fromDate <= f.toDate))")
    Set<BrokerGlobalFee> getApplicableGlobalFeesForAllBrokersAndPeriod(@Param("brokerIds") List<Long> brokerIds,
                                                                       @Param("fromDate") LocalDate from,
                                                                       @Param("toDate") LocalDate to);

    List<BrokerGlobalFee> findAllByBrokerId(Long brokerId);
}
