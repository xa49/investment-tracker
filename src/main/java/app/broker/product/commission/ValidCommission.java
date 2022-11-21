package app.broker.product.commission;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ProductCommissionValidator.class)
public @interface ValidCommission {
    String message() default "ProductCommission must have either percent fee or minimum fee filled. Currency is compulsory.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
