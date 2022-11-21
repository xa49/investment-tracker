package app.broker;

import javax.validation.ConstraintValidatorContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class BrokerEntityValidatorHelper {
    private BrokerEntityValidatorHelper(){
    }

    public static boolean areValidityDatesCorrect(
            LocalDate fromDate, LocalDate toDate, ConstraintValidatorContext cvc) {
        if(fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            cvc.buildConstraintViolationWithTemplate("Valid-from date must not be after valid-to date.")
                    .addPropertyNode("fromDate").addConstraintViolation();
             cvc.buildConstraintViolationWithTemplate("Valid-from date must not be after valid-to date.")
                    .addPropertyNode("toDate").addConstraintViolation();
            return false;
        }
        return true;
    }

    public static boolean isAllPresentInFeeGroup(Map<String, Object> elements, ConstraintValidatorContext cvc) {
        AtomicBoolean isValid = new AtomicBoolean(true);
        if (elements.values().stream().anyMatch(Objects::nonNull)) {
            elements.forEach(
                    (name, value) -> {
                        if (value == null) {
                            isValid.set(false);
                            cvc.buildConstraintViolationWithTemplate("Either none or all of the fee details should be present for this fee type.")
                                    .addPropertyNode(name).addConstraintViolation();
                        }
                    }
            );
        }
        return isValid.get();
    }

    public static boolean isMinFeeNotExceedingMaxFee(
            BigDecimal minimumFee, BigDecimal maximumFee, ConstraintValidatorContext cvc) {
        if (minimumFee != null && maximumFee != null && minimumFee.compareTo(maximumFee) > 0) {
            cvc.buildConstraintViolationWithTemplate("Minimum fee must not exceed maximum fee.")
                    .addPropertyNode("minimumFee").addConstraintViolation();
            cvc.buildConstraintViolationWithTemplate("Minimum fee must not exceed maximum fee.")
                    .addPropertyNode("maximumFee").addConstraintViolation();
            return false;
        }
        return true;
    }
}
