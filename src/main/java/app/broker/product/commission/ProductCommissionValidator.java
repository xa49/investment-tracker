package app.broker.product.commission;

import app.broker.BrokerEntityValidatorHelper;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class ProductCommissionValidator implements ConstraintValidator<ValidCommission, CommissionCommand> {
    @Override
    public boolean isValid(CommissionCommand commissionCommand, ConstraintValidatorContext cvc) {
        cvc.disableDefaultConstraintViolation();
        boolean requiredFeeDetailsPresent = areRequiredFeeDetailsPresent(commissionCommand, cvc);
        boolean validityDatesCorrect = areValidityDatesCorrect(commissionCommand, cvc);
        boolean marketPresent = isMarketPresent(commissionCommand.getMarket(), cvc);

        return requiredFeeDetailsPresent && validityDatesCorrect && marketPresent;
    }

    private boolean areRequiredFeeDetailsPresent(CommissionCommand command, ConstraintValidatorContext cvc) {
        boolean valid = true;
        if (command.getPercentFee() == null && command.getMinimumFee() == null) {
            cvc.buildConstraintViolationWithTemplate("Either percent fee or minimum fee must be present.")
                    .addPropertyNode("percentFee").addConstraintViolation();
            cvc.buildConstraintViolationWithTemplate("Either percent fee or minimum fee must be present.")
                    .addPropertyNode("minimumFee").addConstraintViolation();
            valid = false;
        }
        if (command.getCurrency() == null) {
            cvc.buildConstraintViolationWithTemplate("Currency must be present.")
                    .addPropertyNode("currency").addConstraintViolation();
            valid = false;
        }
        if(command.getMinimumFee() != null && command.getMaximumFee() != null
                && command.getMinimumFee().compareTo(command.getMaximumFee()) >0) {
            cvc.buildConstraintViolationWithTemplate("Minimum fee must not exceed maximum fee.")
                    .addPropertyNode("minimumFee").addConstraintViolation();
            cvc.buildConstraintViolationWithTemplate("Minimum fee must not exceed maximum fee.")
                    .addPropertyNode("maximumFee").addConstraintViolation();
            valid = false;
        }
        return valid;
    }

    private boolean areValidityDatesCorrect(CommissionCommand command, ConstraintValidatorContext cvc) {
        return BrokerEntityValidatorHelper.areValidityDatesCorrect(command.getFromDate(), command.getToDate(), cvc);
    }

    private boolean isMarketPresent(String market, ConstraintValidatorContext cvc) {
        if (market == null) {
            cvc.buildConstraintViolationWithTemplate("Market must be present.")
                    .addPropertyNode("market").addConstraintViolation();
            return false;
        }
        return true;
    }
}
