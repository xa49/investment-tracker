package app.taxation.details;

import app.util.BigDecimalConverter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "tax_details")
@Getter
@Setter
@NoArgsConstructor
public class TaxDetails {

    @Id
    @GeneratedValue
    private Long id;

    private String taxResidence;

    private String taxationCurrency;

    @Convert(converter = BigDecimalConverter.class)
    private BigDecimal flatCapitalGainsTaxRate;

    private LocalDate fromDate;
    private LocalDate toDate;

    private Integer lossOffsetYears = 2;
    private Integer lossOffsetCutOffMonth = 1;
    private Integer lossOffsetCutOffDay = 1;

    public void loadFromCommand(TaxDetailsCommand command) {
        taxResidence = command.getTaxResidence();
        taxationCurrency = command.getTaxationCurrency();
        flatCapitalGainsTaxRate = command.getFlatCapitalGainsTaxRate();
        fromDate = command.getFromDate();
        toDate = command.getToDate();
        lossOffsetYears = command.getLossOffsetYears();
        lossOffsetCutOffMonth = command.getLossOffsetCutOffMonth();
        lossOffsetCutOffDay = command.getLossOffsetCutOffDay();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaxDetails that = (TaxDetails) o;
        return Objects.equals(id, that.id) && Objects.equals(taxResidence, that.taxResidence)
                && Objects.equals(taxationCurrency, that.taxationCurrency)
                && Objects.equals(flatCapitalGainsTaxRate, that.flatCapitalGainsTaxRate)
                && Objects.equals(fromDate, that.fromDate) && Objects.equals(toDate, that.toDate)
                && Objects.equals(lossOffsetYears, that.lossOffsetYears)
                && Objects.equals(lossOffsetCutOffMonth, that.lossOffsetCutOffMonth)
                && Objects.equals(lossOffsetCutOffDay, that.lossOffsetCutOffDay);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, taxResidence, taxationCurrency, flatCapitalGainsTaxRate,
                fromDate, toDate, lossOffsetYears, lossOffsetCutOffMonth, lossOffsetCutOffDay);
    }
}
