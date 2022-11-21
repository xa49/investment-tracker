package app.manager.transaction;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Arrays;

public class TransactionValidator implements ConstraintValidator<ValidTransaction, TransactionCommand> {
    private static final String FIELD_REQUIRED = "This field is required for this transaction type: ";

    @Override
    public boolean isValid(TransactionCommand transactionCommand, ConstraintValidatorContext cvc) {
        boolean dateValid = validateDate(transactionCommand, cvc);
        switch (transactionCommand.getTransactionType()) {
            case MONEY_IN -> {
                return validateMoneyIn(transactionCommand, cvc) && dateValid;
            }
            case MONEY_OUT -> {
                return validateMoneyOut(transactionCommand, cvc) && dateValid;
            }
            case ENTER_INVESTMENT, TRANSFER_CASH -> {
                return validateDoubleSided(transactionCommand, cvc) && dateValid;
            }
            case EXIT_INVESTMENT, TRANSFER_SECURITY -> {
                return validateDoubleSidedWithMatching(transactionCommand, cvc) && dateValid;
            }
            case PAY_FEE -> {
                return validatePayFee(transactionCommand, cvc) && dateValid;
            }
            default -> {
                cvc.buildConstraintViolationWithTemplate("Transaction type must be one of: "
                                + Arrays.toString(TransactionType.values()))
                        .addPropertyNode("transactionType").addConstraintViolation();
                return false;
            }
        }
    }

    private boolean validateDate(TransactionCommand transactionCommand, ConstraintValidatorContext cvc) {
        if (transactionCommand.getDate() == null) {
            cvc.buildConstraintViolationWithTemplate("Date is required for all transactions.")
                    .addPropertyNode("date").addConstraintViolation();
            return false;
        }
        return true;
    }

    private boolean validateMoneyIn(TransactionCommand transactionCommand, ConstraintValidatorContext cvc) {
        return areAddValuesPresent(transactionCommand, cvc);
    }

    private boolean validateMoneyOut(TransactionCommand transactionCommand, ConstraintValidatorContext cvc) {
        return areTakeValuesPresent(transactionCommand, cvc);
    }

    private boolean validateDoubleSided(TransactionCommand transactionCommand, ConstraintValidatorContext cvc) {
        boolean takeValuesPresent = areTakeValuesPresent(transactionCommand, cvc);
        boolean addValuesPresent = areAddValuesPresent(transactionCommand, cvc);
        return takeValuesPresent && addValuesPresent;

    }

    private boolean validateDoubleSidedWithMatching(
            TransactionCommand transactionCommand, ConstraintValidatorContext cvc) {
        boolean takeValuesPresent = areTakeValuesPresent(transactionCommand, cvc);
        boolean addValuesPresent = areAddValuesPresent(transactionCommand, cvc);
        boolean matchingStrategyPresent = isMatchingStrategyPresent(transactionCommand, cvc);
        return takeValuesPresent && addValuesPresent && matchingStrategyPresent;
    }

    private boolean validatePayFee(TransactionCommand transactionCommand, ConstraintValidatorContext cvc) {
        boolean feeTypePresent = true;
        if (transactionCommand.getFeeType() == null) {
            cvc.buildConstraintViolationWithTemplate("FeeType is required for fee payments.")
                    .addPropertyNode("feeType").addConstraintViolation();
            feeTypePresent = false;
        }
        return areTakeValuesPresent(transactionCommand, cvc) && feeTypePresent;
    }

    private boolean isMatchingStrategyPresent(TransactionCommand transactionCommand, ConstraintValidatorContext cvc) {
        if (transactionCommand.getMatchingStrategy() == null) {
            cvc.buildConstraintViolationWithTemplate(FIELD_REQUIRED + transactionCommand.getTransactionType())
                    .addPropertyNode("matchingStrategy").addConstraintViolation();
            return false;
        }
        return true;
    }

    private boolean areAddValuesPresent(TransactionCommand transactionCommand, ConstraintValidatorContext cvc) {
        boolean isValid = true;
        String message = FIELD_REQUIRED + transactionCommand.getTransactionType();
        if (transactionCommand.getAddToAccountId() == null) {
            cvc.buildConstraintViolationWithTemplate(message)
                    .addPropertyNode("addToAccountId").addConstraintViolation();
            isValid = false;
        }
        if (transactionCommand.getAssetAddedId() == null) {
            cvc.buildConstraintViolationWithTemplate(message)
                    .addPropertyNode("assetAddedId").addConstraintViolation();
            isValid = false;
        }
        if (transactionCommand.getCountOfAssetAdded() == null) {
            cvc.buildConstraintViolationWithTemplate(message)
                    .addPropertyNode("countOfAssetAdded").addConstraintViolation();
            isValid = false;
        }

        return isValid;
    }

    private boolean areTakeValuesPresent(TransactionCommand transactionCommand, ConstraintValidatorContext cvc) {
        boolean isValid = true;
        String message = FIELD_REQUIRED + transactionCommand.getTransactionType();
        if (transactionCommand.getTakeFromAccountId() == null) {
            cvc.buildConstraintViolationWithTemplate(message)
                    .addPropertyNode("takeFromAccountId").addConstraintViolation();
            isValid = false;
        }
        if (transactionCommand.getAssetTakenId() == null) {
            cvc.buildConstraintViolationWithTemplate(message)
                    .addPropertyNode("assetTakenId").addConstraintViolation();
            isValid = false;
        }
        if (transactionCommand.getCountOfAssetTaken() == null) {
            cvc.buildConstraintViolationWithTemplate(message)
                    .addPropertyNode("countOfAssetTaken").addConstraintViolation();
            isValid = false;
        }

        return isValid;
    }


}
