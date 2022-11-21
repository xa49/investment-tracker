package app.broker.fees.transfer;

import app.util.DateRange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BrokerTransferFeeRepository extends JpaRepository<BrokerTransferFee, Long> {

    @Query("SELECT NEW app.util.DateRange(f.id, f.fromDate, f.toDate) from BrokerTransferFee f " +
            "WHERE f.broker.id = :brokerId " +
            "AND (f.transferredCurrency = :transferredCurrency OR (f.transferredCurrency IS NULL AND :transferredCurrency IS NULL))" +
            "AND ((:fromDate IS NULL AND :toDate IS NULL) " +
            "OR (f.fromDate IS NULL AND (f.toDate IS NULL OR :fromDate IS NULL OR :fromDate <= f.toDate)) " +
            "OR (f.toDate IS NULL AND (:toDate >= f.fromDate OR :toDate IS NULL)) " +
            "OR ((:fromDate IS NULL OR :fromDate <= f.toDate) AND :toDate >= f.fromDate) " +
            "OR (:toDate IS NULL AND :fromDate <= f.toDate))")
    List<DateRange> getOverlappingDates(@Param("brokerId") Long brokerId,
                                        @Param("transferredCurrency") String transferredCurrency,
                                        @Param("fromDate") LocalDate fromDate,
                                        @Param("toDate") LocalDate toDate);

    @Query("SELECT NEW app.util.DateRange(f.id, f.fromDate, f.toDate) from BrokerTransferFee f " +
            "WHERE f.broker.id = :brokerId " +
            "AND (f.transferredCurrency = :transferredCurrency OR (f.transferredCurrency IS NULL AND :transferredCurrency IS NULL)) " +
            "AND f.id <> :excludedId " +
            "AND ((:fromDate IS NULL AND :toDate IS NULL) " +
            "OR (f.fromDate IS NULL AND (f.toDate IS NULL OR :fromDate IS NULL OR :fromDate <= f.toDate)) " +
            "OR (f.toDate IS NULL AND (:toDate >= f.fromDate OR :toDate IS NULL)) " +
            "OR ((:fromDate IS NULL OR :fromDate <= f.toDate) AND :toDate >= f.fromDate) " +
            "OR (:toDate IS NULL AND :fromDate <= f.toDate))")
    List<DateRange> getOtherOverlappingDates(@Param("brokerId") Long brokerId,
                                             @Param("excludedId") Long excludedId,
                                             @Param("transferredCurrency") String transferredCurrency,
                                             @Param("fromDate") LocalDate fromDate,
                                             @Param("toDate") LocalDate toDate);

    @Query("SELECT f FROM BrokerTransferFee f WHERE f.broker.id = :brokerId " +
            "AND (f.transferredCurrency = :transferCurrency OR (:transferCurrency IS NULL and f.transferredCurrency IS NULL)) " +
            "AND ((f.fromDate IS NULL AND f.toDate IS NULL) " +
            "OR (f.fromDate IS NULL AND f.toDate >= :day) " +
            "OR (f.toDate IS NULL AND f.fromDate <= :day) " +
            "OR (f.fromDate <= :day AND f.toDate >= :day))")
    Optional<BrokerTransferFee> getTransferFeeForBrokerAndDayAndCurrency(@Param("brokerId") Long brokerId,
                                                                         @Param("day") LocalDate day,
                                                                         @Param("transferCurrency") String transferCurrency);

    @Query("SELECT COUNT(f) FROM BrokerTransferFee f WHERE f.broker.id = :brokerId AND f.id = :id")
    int countByBrokerIdAndId(@Param("brokerId") Long brokerId, @Param("id") Long id);

    List<BrokerTransferFee> findAllByBrokerId(Long brokerId);
}
