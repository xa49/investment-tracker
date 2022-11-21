package app.broker.product;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BrokerProductDto {
    private Long id;
    private String name;
    private LocalDate fromDate;
    private LocalDate toDate;

    private BigDecimal fixedFeeAmt;
    private String fixedFeeCurrency;
    private String  fixedFeePeriod;

    private BigDecimal balanceFeePercent;
    private BigDecimal balanceFeeMaxAmt;
    private String balanceFeeMaxCurrency;
}
