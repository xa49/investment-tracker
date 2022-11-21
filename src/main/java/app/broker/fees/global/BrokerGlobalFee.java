package app.broker.fees.global;

import app.broker.Broker;
import app.broker.fees.FeePeriod;
import app.util.BigDecimalConverter;
import app.broker.CommandLoadable;
import app.util.Dated;
import app.broker.RequestCommand;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "broker_global_fees")
@Getter
@Setter
@NoArgsConstructor
public class BrokerGlobalFee implements CommandLoadable, Dated {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "broker_id")
    private Broker broker;

    @Convert(converter = BigDecimalConverter.class)
    private BigDecimal globalFixedFeeAmt;
    private String globalFixedFeeCurrency;
    @Enumerated(EnumType.STRING)
    private FeePeriod globalFixedFeePeriod;

    @Convert(converter = BigDecimalConverter.class)
    private BigDecimal fixedFeeGlobalLimit;
    private String fixedFeeGlobalLimitCurrency;
    @Enumerated(EnumType.STRING)
    private FeePeriod fixedFeeGlobalLimitPeriod;

    @Convert(converter = BigDecimalConverter.class)
    private BigDecimal balanceFeeGlobalLimit;
    private String balanceFeeGlobalLimitCurrency;
    @Enumerated(EnumType.STRING)
    private FeePeriod balanceFeeGlobalLimitPeriod;

    private LocalDate fromDate;
    private LocalDate toDate;
    private LocalDate referencePaymentDate;


    public void loadFromCommand(RequestCommand command) {
        GlobalFeeCommand feeCommand = (GlobalFeeCommand) command;

        globalFixedFeeAmt = feeCommand.getGlobalFixedFeeAmt();
        globalFixedFeeCurrency = feeCommand.getGlobalFixedFeeCurrency();
        globalFixedFeePeriod = feeCommand.getGlobalFixedFeePeriod();

        fixedFeeGlobalLimit = feeCommand.getFixedFeeGlobalLimit();
        fixedFeeGlobalLimitCurrency = feeCommand.getFixedFeeGlobalLimitCurrency();
        fixedFeeGlobalLimitPeriod = feeCommand.getFixedFeeGlobalLimitPeriod();

        balanceFeeGlobalLimit = feeCommand.getBalanceFeeGlobalLimit();
        balanceFeeGlobalLimitCurrency = feeCommand.getBalanceFeeGlobalLimitCurrency();
        balanceFeeGlobalLimitPeriod = feeCommand.getBalanceFeeGlobalLimitPeriod();

        fromDate = feeCommand.getFromDate();
        toDate = feeCommand.getToDate();
        referencePaymentDate = feeCommand.getReferencePaymentDate();
    }
}
