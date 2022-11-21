package app.broker.product;

import app.broker.fees.FeePeriod;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ProductCommand {
     String getName();
     LocalDate getFromDate();
     LocalDate getToDate();

     BigDecimal getFixedFeeAmt();
     String getFixedFeeCurrency();
     FeePeriod  getFixedFeePeriod();

     BigDecimal  getBalanceFeePercent();
     BigDecimal  getBalanceFeeMaxAmt();
     String getBalanceFeeCurrency();
     FeePeriod getBalanceFeePeriod();
}
