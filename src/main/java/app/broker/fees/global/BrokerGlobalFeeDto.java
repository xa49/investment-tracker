package app.broker.fees.global;

import app.broker.fees.FeePeriod;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BrokerGlobalFeeDto {
    private Long id;
    private Long brokerId;
    private BigDecimal globalFixedFeeAmt;
    private String globalFixedFeeCurrency;
    private FeePeriod globalFixedFeePeriod;
    private BigDecimal  fixedFeeGlobalLimit;
    private String fixedFeeGlobalLimitCurrency;
    private FeePeriod  fixedFeeGlobalLimitPeriod;
    private BigDecimal  balanceFeeGlobalLimit;
    private String balanceFeeGlobalLimitCurrency;
    private FeePeriod  balanceFeeGlobalLimitPeriod;
    private LocalDate fromDate;
    private LocalDate toDate;
    private LocalDate referencePaymentDate;
}
