package app.broker.fees.transfer;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BrokerTransferFeeValidator.class)
public @interface ValidTransferFee {
    String message() default "BrokerTransferFee must have minimum one of percent fee or minimum fee. Fee currency and transfer currency are compulsory.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
