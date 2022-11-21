package app.broker.account.association;

import app.broker.BrokerEntityValidatorHelper;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class ProductAssociationValidator implements ConstraintValidator<ValidAssociation, ProductAssociationCommand> {
    @Override
    public boolean isValid(ProductAssociationCommand command, ConstraintValidatorContext cvc) {
        return BrokerEntityValidatorHelper.areValidityDatesCorrect(command.getFromDate(), command.getToDate(), cvc);
    }
}
