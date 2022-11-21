package app.data.fx.rate;

import app.util.BigDecimalConverter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "exchange_rates")
@Getter
@Setter
@NoArgsConstructor
public class StoredRate implements Rate {
    @Id
    @GeneratedValue(generator = "rate_seq_gen")
    @SequenceGenerator(name = "rate_seq_gen", sequenceName = "rate_seq")
    private Long id;

    private String sourceIsoAbbreviation;
    private String destinationIsoAbbreviation;
    private LocalDate date;
    @Convert(converter = BigDecimalConverter.class)
    private BigDecimal exchangeRate;

    public StoredRate(String sourceIsoAbbreviation, String destinationIsoAbbreviation, LocalDate date,
                      BigDecimal exchangeRate) {
        this.sourceIsoAbbreviation = sourceIsoAbbreviation;
        this.destinationIsoAbbreviation = destinationIsoAbbreviation;
        this.date = date;
        this.exchangeRate = exchangeRate;
    }

    @Override
    public String toString() {
        return "BasicStoredRate{" +
                "id=" + id +
                ", sourceCurrency=" + sourceIsoAbbreviation +
                ", destinationCurrency=" + destinationIsoAbbreviation +
                ", date=" + date +
                ", exchangeRate=" + exchangeRate +
                '}';
    }
}
