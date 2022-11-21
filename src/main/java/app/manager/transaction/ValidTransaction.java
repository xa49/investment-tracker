package app.manager.transaction;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TransactionValidator.class)
public @interface ValidTransaction {
    String message() default "Transaction must contain all required info.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
