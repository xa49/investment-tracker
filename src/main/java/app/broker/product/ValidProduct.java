package app.broker.product;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BrokerProductValidator.class)
public @interface ValidProduct {
    String message() default "BrokerProduct fee groups must either be empty or complete.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
