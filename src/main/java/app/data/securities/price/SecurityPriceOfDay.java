package app.data.securities.price;

import app.util.BigDecimalConverter;
import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "security_prices")
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class SecurityPriceOfDay implements SecurityPrice {
    @Id
    @GeneratedValue
    private Long id;

    private String ticker;

    private LocalDate date;

    private String currency;

    @Convert(converter = BigDecimalConverter.class)
    private BigDecimal open;

    @Convert(converter = BigDecimalConverter.class)
    private BigDecimal close;

    @Convert(converter = BigDecimalConverter.class)
    private BigDecimal adjClose;

    @Convert(converter = BigDecimalConverter.class)
    private BigDecimal low;

    @Convert(converter = BigDecimalConverter.class)
    private BigDecimal high;

    private long volume;

    @Override
    public BigDecimal getPrice() {
        return close;
    }

    @Override
    public String toString() {
        return "DaysPrice{" +
                "ticker='" + ticker + '\'' +
                ", date=" + date +
                ", close=" + close +
                '}';
    }
}
