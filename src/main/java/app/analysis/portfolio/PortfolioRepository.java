package app.analysis.portfolio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {
    int countByName(String name);

    @Query("SELECT p FROM Portfolio p LEFT JOIN FETCH p.accountIds WHERE p.name = :name")
    Optional<Portfolio> getPortfolioByName(String name);

}
