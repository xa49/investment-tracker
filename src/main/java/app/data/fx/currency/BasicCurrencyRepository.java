package app.data.fx.currency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BasicCurrencyRepository extends JpaRepository<BasicCurrency, Long> {
    Optional<BasicCurrency> findByIsoCode(String isoCode);
}
