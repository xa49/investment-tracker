package app.broker.fees.transfer;

import app.broker.BrokerEntityValidatorHelper;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.math.BigDecimal;

public class BrokerTransferFeeValidator implements ConstraintValidator<ValidTransferFee, TransferFeeCommand> {

    @Override
    public boolean isValid(TransferFeeCommand transferFeeCommand, ConstraintValidatorContext cvc) {
        cvc.disableDefaultConstraintViolation();
        boolean pricePresent =
                isPricePresent(transferFeeCommand.getPercentFee(), transferFeeCommand.getMinimumFee(), cvc);
        boolean transferCurrencyPresent =
                isCurrencyPresent(transferFeeCommand.getTransferredCurrency(), "transferredCurrency", cvc);
        boolean feeCurrencyPresent =
                isCurrencyPresent(transferFeeCommand.getFeeCurrency(), "feeCurrency", cvc);
        boolean minNotExceedingMax =
                BrokerEntityValidatorHelper.isMinFeeNotExceedingMaxFee(
                        transferFeeCommand.getMinimumFee(), transferFeeCommand.getMaximumFee(), cvc);
        boolean validityDatesCorrect =
                BrokerEntityValidatorHelper.areValidityDatesCorrect(
                        transferFeeCommand.getFromDate(), transferFeeCommand.getToDate(), cvc);

        return pricePresent && transferCurrencyPresent && feeCurrencyPresent && minNotExceedingMax && validityDatesCorrect;
    }

    private boolean isPricePresent(BigDecimal percentBased, BigDecimal minimumFee, ConstraintValidatorContext cvc) {
        if (percentBased == null && minimumFee == null) {
            cvc.buildConstraintViolationWithTemplate("Either minimum fee or percent based fee must be present.")
                    .addPropertyNode("minimumFee").addConstraintViolation();
            cvc.buildConstraintViolationWithTemplate("Either minimum fee or percent based fee must be present.")
                    .addPropertyNode("percentFee").addConstraintViolation();
            return false;
        }
        return true;
    }

    private boolean isCurrencyPresent(String value, String currencyName, ConstraintValidatorContext cvc) {
        if (value == null) {
            cvc.buildConstraintViolationWithTemplate(currencyName + " must be present.")
                    .addPropertyNode(currencyName).addConstraintViolation();
            return false;
        }
        return true;
    }
}
