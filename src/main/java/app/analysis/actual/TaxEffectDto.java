package app.analysis.actual;

import app.analysis.CashValue;
import app.analysis.DatedCashValue;
import app.analysis.liquid.IntermediateTaxEffectDto;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
public class TaxEffectDto {
    private final CashValue taxPaid;
    private final CashValue lossAdded;
    private final List<DatedCashValue> lossesUsed;
    private final CashValue feeUsed;
    private final LocalDate transactionDate;

    public static TaxEffectDto fromIntermediateDto(IntermediateTaxEffectDto dto, CashValue taxPaid) {
        return new TaxEffectDto(taxPaid, dto.getLossAdded(), dto.getLossesUsed(), dto.getFeeUsed(),
                dto.getTransactionDate());
    }
}
