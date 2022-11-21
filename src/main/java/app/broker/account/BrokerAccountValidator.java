package app.broker.account;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.time.LocalDate;

public class BrokerAccountValidator implements ConstraintValidator<ValidAccount, AccountCommand> {
    @Override
    public boolean isValid(AccountCommand accountCommand, ConstraintValidatorContext cvc) {
        boolean validityDatesCorrect =
                areValidityDatesCorrect(accountCommand.getOpenedDate(), accountCommand.getClosedDate(), cvc);
        boolean namePresent = isNamePresent(accountCommand, cvc);
        return validityDatesCorrect && namePresent;
    }

    private boolean isNamePresent(AccountCommand accountCommand, ConstraintValidatorContext cvc) {
        if(accountCommand.getName() == null) {
            cvc.buildConstraintViolationWithTemplate("Account open date must be specified.")
                    .addPropertyNode("name").addConstraintViolation();
            return false;
        }
        return true;
    }

    private boolean areValidityDatesCorrect(
            LocalDate openedDate, LocalDate closedDate, ConstraintValidatorContext cvc) {
        boolean isValid = true;
        if(openedDate == null) {
            cvc.buildConstraintViolationWithTemplate("Account open date must be specified.")
                    .addPropertyNode("openedDate").addConstraintViolation();
            isValid = false;
        }
        if(openedDate != null && closedDate != null && openedDate.isAfter(closedDate)) {
            cvc.buildConstraintViolationWithTemplate("Opened date must not be after closed date.")
                    .addPropertyNode("openedDate").addConstraintViolation();
            cvc.buildConstraintViolationWithTemplate("Opened date must not be after closed date.")
                    .addPropertyNode("closedDate").addConstraintViolation();
            isValid = false;
        }
        return isValid;
    }
}
