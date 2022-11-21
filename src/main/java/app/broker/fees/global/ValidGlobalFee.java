package app.broker.fees.global;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BrokerGlobalFeeValidator.class)
public @interface ValidGlobalFee {
    String message() default "BrokerGlobalFee must have complete groups of fee specifications.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
