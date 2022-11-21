package app.taxation.details;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TaxDetailsRepository extends JpaRepository<TaxDetails, Long> {
    @Query("SELECT d FROM TaxDetails d WHERE d.taxResidence = :taxResidence " +
            "AND ((d.fromDate IS NULL OR d.fromDate <= :date) AND (d.toDate IS NULL OR d.toDate >= :date))")
    Optional<TaxDetails> getForResidenceAndDate(
            @Param("taxResidence") String taxResidence, @Param("date") LocalDate asOfDate);

    @Query("SELECT d FROM TaxDetails d WHERE d.taxResidence = :taxResidence " +
            "AND ((:fromDate IS NULL AND :toDate IS NULL) " +
            "OR (d.fromDate IS NULL AND (d.toDate IS NULL OR :fromDate IS NULL OR :fromDate < d.toDate)) " +
            "OR (d.toDate IS NULL AND (:toDate > d.fromDate OR :toDate IS NULL)) " +
            "OR ((:fromDate IS NULL OR :fromDate < d.toDate) AND :toDate > d.fromDate) " +
            "OR (:toDate IS NULL AND :fromDate < d.toDate))")
    List<TaxDetails> findForResidenceAndDate(
            @Param("taxResidence") String taxResidence, @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query("SELECT d FROM TaxDetails d WHERE d.taxResidence = :taxResidence " +
            "AND d.id <> :excludedId " +
            "AND ((:fromDate IS NULL AND :toDate IS NULL) " +
            "OR (d.fromDate IS NULL AND (d.toDate IS NULL OR :fromDate IS NULL OR :fromDate < d.toDate)) " +
            "OR (d.toDate IS NULL AND (:toDate > d.fromDate OR :toDate IS NULL)) " +
            "OR ((:fromDate IS NULL OR :fromDate < d.toDate) AND :toDate > d.fromDate) " +
            "OR (:toDate IS NULL AND :fromDate < d.toDate))")
    List<TaxDetails> findOthersForResidenceAndDate(
            @Param("taxResidence") String taxResidence, @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate, @Param("excludedId") Long excludedId);
}
