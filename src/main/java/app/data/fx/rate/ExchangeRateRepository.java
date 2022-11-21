package app.data.fx.rate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface ExchangeRateRepository extends JpaRepository<StoredRate, Long> {
    @Query("SELECT r FROM StoredRate r WHERE r.sourceIsoAbbreviation = :sourceAbbreviation " +
            "AND r.destinationIsoAbbreviation = :destinationAbbreviation AND r.date = :date")
    Optional<Rate> findRate(@Param("sourceAbbreviation") String sourceAbbreviation,
                            @Param("destinationAbbreviation") String destinationAbbreviation,
                            @Param("date") LocalDate date);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM StoredRate r " +
            "WHERE r.sourceIsoAbbreviation = :sourceAbbreviation " +
            "AND r.destinationIsoAbbreviation = :destinationAbbreviation AND r.date = :date")
    boolean isAlreadyStored(@Param("sourceAbbreviation") String sourceAbbreviation,
                            @Param("destinationAbbreviation") String destinationAbbreviation,
                            @Param("date") LocalDate date);
}
