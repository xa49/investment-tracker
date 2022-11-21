package app.data.fx.rate;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface Rate {
    String getSourceIsoAbbreviation();
    String getDestinationIsoAbbreviation();
    LocalDate getDate();
    BigDecimal getExchangeRate();
}
