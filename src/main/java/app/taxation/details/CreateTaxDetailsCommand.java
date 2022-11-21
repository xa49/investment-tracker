package app.taxation.details;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class CreateTaxDetailsCommand implements TaxDetailsCommand{
    private String taxResidence;
    private String taxationCurrency;
    private BigDecimal flatCapitalGainsTaxRate;
    private LocalDate fromDate;
    private LocalDate toDate;
    private Integer lossOffsetYears;
    private Integer lossOffsetCutOffMonth;
    private Integer lossOffsetCutOffDay;
}
