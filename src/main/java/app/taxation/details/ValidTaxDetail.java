package app.taxation.details;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TaxDetailsValidator.class)
public @interface ValidTaxDetail {

    String message() default "You need to provide valid tax details.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
