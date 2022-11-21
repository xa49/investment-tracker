package app.broker.account;

import app.util.DateRange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BrokerAccountRepository extends JpaRepository<BrokerAccount, Long> {
    int countById(Long id);

    @Query("SELECT NEW app.util.DateRange(a.id, a.openedDate, a.closedDate) from BrokerAccount a " +
            "WHERE a.name = :name " +
            "AND ((:openedDate IS NULL AND :closedDate IS NULL) " +
            "OR (a.openedDate IS NULL AND (a.closedDate IS NULL OR :openedDate IS NULL OR :openedDate <= a.closedDate)) " +
            "OR (a.closedDate IS NULL AND (:closedDate >= a.openedDate OR :closedDate IS NULL)) " +
            "OR ((:openedDate IS NULL OR :openedDate <= a.closedDate) AND :closedDate >= a.openedDate) " +
            "OR (:closedDate IS NULL AND :openedDate <= a.closedDate))")
    List<DateRange> getOverlappingAccounts(
            @Param("name") String name, @Param("openedDate") LocalDate openedDate,
            @Param("closedDate") LocalDate closedDate);

    @Query("SELECT NEW app.util.DateRange(a.id, a.openedDate, a.closedDate) from BrokerAccount a " +
            "WHERE a.name = :name AND a.id <> :id " +
            "AND ((:openedDate IS NULL AND :closedDate IS NULL) " +
            "OR (a.openedDate IS NULL AND (a.closedDate IS NULL OR :openedDate IS NULL OR :openedDate <= a.closedDate)) " +
            "OR (a.closedDate IS NULL AND (:closedDate >= a.openedDate OR :closedDate IS NULL)) " +
            "OR ((:openedDate IS NULL OR :openedDate <= a.closedDate) AND :closedDate >= a.openedDate) " +
            "OR (:closedDate IS NULL AND :openedDate <= a.closedDate))")
    List<DateRange> getOtherOverlappingAccounts(@Param("id") Long excludeId, String name, LocalDate openedDate,
            LocalDate closedDate);

}
