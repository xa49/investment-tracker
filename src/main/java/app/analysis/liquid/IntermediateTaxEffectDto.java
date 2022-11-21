package app.analysis.liquid;

import app.analysis.CashValue;
import app.analysis.DatedCashValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor
public class IntermediateTaxEffectDto {
    private CashValue taxableGain;
    private CashValue lossAdded;
    private List<DatedCashValue> lossesUsed;
    private CashValue feeUsed;
    private LocalDate transactionDate;
}
