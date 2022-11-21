package app.broker.fees.transfer;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@ValidTransferFee
public class UpdateBrokerTransferFeeCommand implements TransferFeeCommand {
    private String transferredCurrency;
    private LocalDate fromDate;
    private LocalDate toDate;
    private BigDecimal percentFee;
    private BigDecimal minimumFee;
    private BigDecimal maximumFee;
    private String feeCurrency;
}
