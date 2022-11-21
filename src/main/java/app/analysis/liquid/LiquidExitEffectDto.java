package app.analysis.liquid;

import app.analysis.CashValue;
import app.analysis.CashValueWithTaxDetails;
import app.broker.account.BrokerAccount;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
public class LiquidExitEffectDto {
    private BrokerAccount account;
    private CashValue grossProceeds;
    private CashValueWithTaxDetails fees;
    private CashValue untaxedGain;
    private CashValue lossesAdded;
}
