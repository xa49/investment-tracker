package app.taxation.details;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TaxDetailsDto {
    private Long id;
    private String taxResidence;
    private String taxationCurrency;
    private BigDecimal flatCapitalGainsTaxRate;
    private LocalDate fromDate;
    private LocalDate toDate;
    private Integer lossOffsetYears;
    private Integer lossOffsetCutOffMonth;
    private Integer lossOffsetCutOffDay;
}
