package app.data.securities.price;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface SecurityPrice {
    String getTicker();
    BigDecimal getPrice();
    String getCurrency();
    LocalDate getDate();
}
