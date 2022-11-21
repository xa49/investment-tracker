package app.broker.product;

import app.broker.fees.FeePeriod;
import app.broker.RequestCommand;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@ValidProduct
public class UpdateBrokerProductCommand implements ProductCommand, RequestCommand {
    private String name;
    private LocalDate fromDate;
    private LocalDate toDate;

    private BigDecimal fixedFeeAmt;
    private String fixedFeeCurrency;
    private FeePeriod  fixedFeePeriod;

    private BigDecimal  balanceFeePercent;
    private BigDecimal  balanceFeeMaxAmt;
    private String balanceFeeCurrency;
    private FeePeriod balanceFeePeriod;
}
