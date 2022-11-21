package app.broker.product.commission;

import app.broker.RequestCommand;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface CommissionCommand extends RequestCommand {
     String getMarket();

     BigDecimal getPercentFee();
     BigDecimal getMinimumFee();
     BigDecimal getMaximumFee();
     String getCurrency();

     LocalDate getFromDate();
     LocalDate getToDate();
}
