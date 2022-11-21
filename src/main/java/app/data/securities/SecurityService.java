package app.data.securities;


import app.data.securities.price.SecurityPrice;
import app.data.securities.security.Security;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SecurityService {
    Optional<SecurityPrice> getPrice(String ticker, LocalDate date);
    Optional<SecurityPrice> getPrice(String ticker, LocalDate date, int backwardDaysOffsetTolerance);
    Optional<String> getMarketBySecurityId(Long securityId);
    Optional<String> getMarketByTicker(String ticker);

    Optional<Security> getSecurityByTicker(String ticker);

    Optional<Security> getSecurityById(Long securityId);

    List<Security> getAllByIdList(List<Long> currencyIds);
}
