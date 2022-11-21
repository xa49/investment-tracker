package app.analysis.actual;

import app.analysis.CashValue;
import app.analysis.DatedCashValue;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ActualPositionOverviewDto {
    private Map<String, Map<String, BigDecimal>> cashBalance;
    private Map<String , Map<String, BigDecimal>> securityBalance;
    private Map<String, BigDecimal> bankBalance;
    private CashValue feeWriteOffAvailable;
    private List<DatedCashValue> lossOffsetsRecorded;
}
