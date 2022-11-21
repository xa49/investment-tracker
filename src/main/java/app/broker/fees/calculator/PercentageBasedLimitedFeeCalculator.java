package app.broker.fees.calculator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class PercentageBasedLimitedFeeCalculator {

    public BigDecimal calculateFee(BigDecimal baseAmountInFeeCurrency, BigDecimal percentage, BigDecimal minimumFee,
                                   BigDecimal maximumFee) {
        if (percentage == null && minimumFee == null) {
            log.info("Percent fee and minimum fee are both missing despite validations.");
            throw new IllegalStateException("Both percent fee and minimum fee are missing for fee. At least one must be present."); // make this handled own exception
        }

        if (percentage != null) {
            BigDecimal percentageBasedFee = baseAmountInFeeCurrency.multiply(percentage).multiply(new BigDecimal("0.01"));

            return limitFee(minimumFee, maximumFee, percentageBasedFee);
        }
        return minimumFee;
    }

    private BigDecimal limitFee(BigDecimal minimum, BigDecimal maximum, BigDecimal actual) {
        if (minimum == null && maximum == null) {
            return actual;
        }
        if (minimum == null) {
            return maximum.min(actual);
        }
        if (maximum == null) {
            return minimum.max(actual);
        }
        return actual.max(minimum).min(maximum);
    }
}
