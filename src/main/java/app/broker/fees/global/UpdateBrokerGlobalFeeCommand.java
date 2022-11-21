package app.broker.fees.global;

import app.broker.fees.FeePeriod;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@ValidGlobalFee
public class UpdateBrokerGlobalFeeCommand implements GlobalFeeCommand {
    private BigDecimal globalFixedFeeAmt;
    private String globalFixedFeeCurrency;
    private FeePeriod globalFixedFeePeriod;

    private BigDecimal fixedFeeGlobalLimit;
    private String fixedFeeGlobalLimitCurrency;
    private FeePeriod fixedFeeGlobalLimitPeriod;

    private BigDecimal balanceFeeGlobalLimit;
    private String balanceFeeGlobalLimitCurrency;
    private FeePeriod balanceFeeGlobalLimitPeriod;

    private LocalDate fromDate;
    private LocalDate toDate;
    private LocalDate referencePaymentDate;
}
