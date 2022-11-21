package app.broker.fees.global;

import app.broker.fees.FeePeriod;
import app.broker.RequestCommand;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface GlobalFeeCommand extends RequestCommand {
    BigDecimal getGlobalFixedFeeAmt();
    String getGlobalFixedFeeCurrency();
    FeePeriod getGlobalFixedFeePeriod();

    BigDecimal getFixedFeeGlobalLimit();
    String getFixedFeeGlobalLimitCurrency();
    FeePeriod  getFixedFeeGlobalLimitPeriod();

    BigDecimal getBalanceFeeGlobalLimit();
    String getBalanceFeeGlobalLimitCurrency();
    FeePeriod  getBalanceFeeGlobalLimitPeriod();

    LocalDate getFromDate();
    LocalDate getToDate();
    LocalDate getReferencePaymentDate();
}
