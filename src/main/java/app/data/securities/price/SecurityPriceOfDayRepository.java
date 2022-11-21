package app.data.securities.price;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface SecurityPriceOfDayRepository extends JpaRepository<SecurityPriceOfDay, Long> {
    Optional<SecurityPrice> findByTickerAndDate(String ticker, LocalDate date);

    long countByTickerAndDate(String ticker, LocalDate date);
}
