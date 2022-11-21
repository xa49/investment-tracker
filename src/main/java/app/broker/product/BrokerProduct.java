package app.broker.product;

import app.broker.Broker;
import app.broker.fees.FeePeriod;
import app.broker.CommandLoadable;
import app.broker.RequestCommand;
import app.util.BigDecimalConverter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "broker_products")
@Getter
@Setter
@NoArgsConstructor
public class BrokerProduct implements CommandLoadable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "broker_id")
    private Broker broker;

    private String name;
    private LocalDate fromDate;
    private LocalDate toDate;

    @Convert(converter = BigDecimalConverter.class)
    private BigDecimal fixedFeeAmt;
    private String fixedFeeCurrency;
    @Enumerated(EnumType.STRING)
    private FeePeriod fixedFeePeriod;

    @Convert(converter = BigDecimalConverter.class)
    private BigDecimal balanceFeePercent;
    @Convert(converter =  BigDecimalConverter.class)
    private BigDecimal balanceFeeMaxAmt;
    private String balanceFeeCurrency;
    private FeePeriod balanceFeePeriod;

    @Override
    public void loadFromCommand(RequestCommand command) {
        ProductCommand productCommand = (ProductCommand) command;

        name = productCommand.getName();
        fromDate = productCommand.getFromDate();
        toDate = productCommand.getToDate();

        fixedFeeAmt = productCommand.getFixedFeeAmt();
        fixedFeeCurrency = productCommand.getFixedFeeCurrency();
        fixedFeePeriod = productCommand.getFixedFeePeriod();

        balanceFeePercent = productCommand.getBalanceFeePercent();
        balanceFeeMaxAmt = productCommand.getBalanceFeeMaxAmt();
        balanceFeeCurrency = productCommand.getBalanceFeeCurrency();
        balanceFeePeriod = productCommand.getBalanceFeePeriod();
    }

}
