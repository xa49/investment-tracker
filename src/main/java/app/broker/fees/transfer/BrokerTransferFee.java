package app.broker.fees.transfer;

import app.broker.Broker;
import app.broker.CommandLoadable;
import app.util.Dated;
import app.broker.RequestCommand;
import app.util.BigDecimalConverter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "broker_transfer_fees")
@Getter
@Setter
@NoArgsConstructor
public class BrokerTransferFee implements CommandLoadable, Dated {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "broker_id")
    private Broker broker;

    private String transferredCurrency;

    private LocalDate fromDate;

    private LocalDate toDate;

    @Convert(converter = BigDecimalConverter.class)
    private BigDecimal percentFee;

    @Convert(converter = BigDecimalConverter.class)
    private BigDecimal minimumFee;

    @Convert(converter = BigDecimalConverter.class)
    private BigDecimal maximumFee;

    private String feeCurrency;

    @Override
    public void loadFromCommand(RequestCommand command) {
        TransferFeeCommand transferFeeCommand = (TransferFeeCommand) command;
        transferredCurrency = transferFeeCommand.getTransferredCurrency();
        fromDate = transferFeeCommand.getFromDate();
        toDate = transferFeeCommand.getToDate();

        percentFee = transferFeeCommand.getPercentFee();
        minimumFee = transferFeeCommand.getMinimumFee();
        maximumFee = transferFeeCommand.getMaximumFee();
        feeCurrency = transferFeeCommand.getFeeCurrency();
    }

}
