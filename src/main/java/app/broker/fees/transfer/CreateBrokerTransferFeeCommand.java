package app.broker.fees.transfer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidTransferFee
public class CreateBrokerTransferFeeCommand implements TransferFeeCommand {
    private String transferredCurrency;
    private LocalDate fromDate;
    private LocalDate toDate;
    private BigDecimal percentFee;
    private BigDecimal minimumFee;
    private BigDecimal maximumFee;
    private String feeCurrency;
}
