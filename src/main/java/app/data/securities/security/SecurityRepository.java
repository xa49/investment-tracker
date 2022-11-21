package app.data.securities.security;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SecurityRepository extends CrudRepository<Security, Long> {

    Optional<Security> findByTicker(String ticker);

    int countByTicker(String ticker);

    @Query("SELECT s.market FROM Security s WHERE s.id = :securityId")
    Optional<String> getMarketById(@Param("securityId") Long securityId);

    @Query("SELECT s.market FROM Security s WHERE s.ticker = :ticker")
    Optional<String> getMarketByTicker(@Param("ticker") String ticker);
}
