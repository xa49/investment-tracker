package app.broker.account;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BrokerAccountValidator.class)
public @interface ValidAccount {
    String message() default "BrokerAccount opened date must not be after closed date.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
