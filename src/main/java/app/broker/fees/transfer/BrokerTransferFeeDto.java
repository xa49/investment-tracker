package app.broker.fees.transfer;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BrokerTransferFeeDto {
    private Long id;

    private Long brokerId;

    private String transferredCurrency;

    private LocalDate fromDate;

    private LocalDate toDate;

    private BigDecimal percentFee;

    private BigDecimal  minimumFee;

    private BigDecimal  maximumFee;

    private String feeCurrency;
}
