package app.broker.product;

import app.broker.fees.FeePeriod;
import app.broker.BrokerEntityValidatorHelper;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class BrokerProductValidator implements ConstraintValidator<ValidProduct, ProductCommand> {
    @Override
    public boolean isValid(ProductCommand productCommand, ConstraintValidatorContext cvc) {
        boolean namePresent = isNamePresent(productCommand, cvc);
        boolean fixedFeeDetailsCorrect = areBalanceFeeDetailsCorrect(productCommand, cvc);
        boolean balanceFeeDetailsCorrect =
                areBalanceFeeDetailsCorrect(productCommand.getBalanceFeePercent(),
                        productCommand.getBalanceFeeCurrency(), productCommand.getBalanceFeePeriod(), cvc);
        boolean validityDatesCorrect =
                BrokerEntityValidatorHelper.areValidityDatesCorrect(productCommand.getFromDate(),
                        productCommand.getToDate(), cvc);
        return namePresent && fixedFeeDetailsCorrect && balanceFeeDetailsCorrect && validityDatesCorrect;
    }

    private boolean isNamePresent(ProductCommand productCommand, ConstraintValidatorContext cvc) {
        if (productCommand.getName() == null) {
            cvc.buildConstraintViolationWithTemplate("Every product must have a name.")
                    .addPropertyNode("name").addConstraintViolation();
            return false;
        }
        return true;
    }

    private boolean areBalanceFeeDetailsCorrect(
            BigDecimal percent, String currency, FeePeriod period, ConstraintValidatorContext cvc) {
        boolean isValid = true;
        if (percent != null || currency != null || period != null) {
            String message = "This field should be present for this fee type.";
            if (percent == null) {
                cvc.buildConstraintViolationWithTemplate(message)
                        .addPropertyNode("balanceFeePercent").addConstraintViolation();
                isValid = false;
            }
            if (currency == null) {
                cvc.buildConstraintViolationWithTemplate(message)
                        .addPropertyNode("balanceFeeCurrency").addConstraintViolation();
                isValid = false;
            }
            if (period == null) {
                cvc.buildConstraintViolationWithTemplate(message)
                        .addPropertyNode("balanceFeePeriod").addConstraintViolation();
                isValid = false;
            }
        }
        return isValid;
    }

    private boolean areBalanceFeeDetailsCorrect(ProductCommand productCommand, ConstraintValidatorContext cvc) {
        Map<String, Object> elements = new HashMap<>();
        elements.put("fixedFeeAmt", productCommand.getFixedFeeAmt());
        elements.put("fixedFeeCurrency", productCommand.getFixedFeeCurrency());
        elements.put("fixedFeePeriod", productCommand.getFixedFeePeriod());
        return BrokerEntityValidatorHelper.isAllPresentInFeeGroup(elements, cvc);
    }

}
