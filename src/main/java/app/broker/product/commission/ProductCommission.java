package app.broker.product.commission;

import app.broker.product.BrokerProduct;
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
@Table(name = "broker_product_commissions")
@Getter
@Setter
@NoArgsConstructor
public class ProductCommission implements CommandLoadable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private BrokerProduct product;

    private String market;

    @Convert(converter = BigDecimalConverter.class)
    private BigDecimal percentFee;
    @Convert(converter = BigDecimalConverter.class)
    private BigDecimal minimumFee;
    @Convert(converter = BigDecimalConverter.class)
    private BigDecimal maximumFee;
    private String currency;

    private LocalDate fromDate;
    private LocalDate toDate;

    @Override
    public void loadFromCommand(RequestCommand command) {
        CommissionCommand productCommand = (CommissionCommand) command;

        market = productCommand.getMarket();
        percentFee = productCommand.getPercentFee();
        minimumFee = productCommand.getMinimumFee();
        maximumFee = productCommand.getMaximumFee();
        currency = productCommand.getCurrency();
        fromDate = productCommand.getFromDate();
        toDate = productCommand.getToDate();
    }
}
