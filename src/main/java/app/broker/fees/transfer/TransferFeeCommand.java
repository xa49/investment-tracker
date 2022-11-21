package app.broker.fees.transfer;

import app.broker.RequestCommand;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface TransferFeeCommand extends RequestCommand {
     String getTransferredCurrency();
     LocalDate getFromDate();
     LocalDate getToDate();
     BigDecimal getPercentFee();
     BigDecimal getMinimumFee();
     BigDecimal getMaximumFee();
     String getFeeCurrency();
}
