package app.taxation.details;

import java.math.BigDecimal;
import java.time.LocalDate;

@ValidTaxDetail
public interface TaxDetailsCommand {
    String getTaxResidence();
    String getTaxationCurrency();
    BigDecimal getFlatCapitalGainsTaxRate();

    LocalDate getFromDate();

    LocalDate getToDate();
    Integer getLossOffsetYears();

    Integer getLossOffsetCutOffMonth();

    Integer getLossOffsetCutOffDay();
}
