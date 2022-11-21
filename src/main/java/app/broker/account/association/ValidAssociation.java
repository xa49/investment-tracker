package app.broker.account.association;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ProductAssociationValidator.class)
public @interface ValidAssociation {
    String message() default "ProductAccountAssociation must have valid dates.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
