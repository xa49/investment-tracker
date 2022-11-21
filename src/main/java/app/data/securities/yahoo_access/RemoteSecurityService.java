package app.data.securities.yahoo_access;

import app.data.securities.price.SecurityPrice;
import app.data.securities.security.Security;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

public interface RemoteSecurityService {
    Map<LocalDate, SecurityPrice> getPrices(String  ticker, LocalDate from, LocalDate to);
    Optional<Security> getSecurityDetails(String ticker);
}
