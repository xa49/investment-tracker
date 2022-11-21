package app.taxation.details;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Map;

public class TaxDetailsValidator implements ConstraintValidator<ValidTaxDetail, TaxDetailsCommand> {
    private static final String MONTH_FIELD_NAME = "lossOffsetCutOffMonth";
    private static final String DAY_FIELD_NAME = "lossOffsetCutOffDay";

    @Override
    public boolean isValid(TaxDetailsCommand taxDetails, ConstraintValidatorContext constraintValidatorContext) {
        boolean residencePresent = isResidencePresent(taxDetails, constraintValidatorContext);
        boolean taxCurrencyPresent = isTaxCurrencyPresent(taxDetails, constraintValidatorContext);
        boolean flatRatePresent = isFlatRatePresent(taxDetails, constraintValidatorContext);
        boolean datesCorrect = areDatesCorrect(taxDetails, constraintValidatorContext);
        boolean lossOffsetValuesValid = areLossOffsetValuesValid(taxDetails, constraintValidatorContext);
        return residencePresent && taxCurrencyPresent && flatRatePresent && datesCorrect && lossOffsetValuesValid;
    }

    private boolean isResidencePresent(TaxDetailsCommand taxDetails, ConstraintValidatorContext cvc) {
        if (taxDetails.getTaxResidence() == null) {
            cvc.buildConstraintViolationWithTemplate("Tax residence must be filled.")
                    .addPropertyNode("taxResidence").addConstraintViolation();
            return false;
        }
        return true;
    }

    private boolean isTaxCurrencyPresent(TaxDetailsCommand taxDetails, ConstraintValidatorContext cvc) {
        if (taxDetails.getTaxationCurrency() == null || taxDetails.getTaxationCurrency().isBlank()) {
            cvc.buildConstraintViolationWithTemplate("Tax currency must be filled.")
                    .addPropertyNode("taxationCurrency").addConstraintViolation();
            return false;
        }
        return true;
    }

    private boolean isFlatRatePresent(TaxDetailsCommand taxDetails, ConstraintValidatorContext cvc) {
        if (taxDetails.getFlatCapitalGainsTaxRate() == null) {
            cvc.buildConstraintViolationWithTemplate("Flat tax rate must be filled.")
                    .addPropertyNode("flatCapitalGainsTaxRate").addConstraintViolation();
            return false;
        }
        return true;
    }

    private boolean areDatesCorrect(TaxDetailsCommand taxDetails, ConstraintValidatorContext cvc) {
        if (taxDetails.getFromDate() != null && taxDetails.getToDate() != null
                && taxDetails.getFromDate().isAfter(taxDetails.getToDate())) {
            cvc.buildConstraintViolationWithTemplate("From date must not be after to date.")
                    .addPropertyNode("fromDate").addConstraintViolation();
            cvc.buildConstraintViolationWithTemplate("From date must not be after to date.")
                    .addPropertyNode("toDate").addConstraintViolation();
            return false;
        }
        return true;
    }

    private boolean areLossOffsetValuesValid(TaxDetailsCommand taxDetails, ConstraintValidatorContext cvc) {
        boolean valid = true;
        if (taxDetails.getLossOffsetYears() != null) {
            valid = areCutOffDatesPresent(taxDetails, cvc);

            if (taxDetails.getLossOffsetYears() < 0) {
                cvc.buildConstraintViolationWithTemplate("Loss offset years must not be negative.")
                        .addPropertyNode("lossOffsetYears").addConstraintViolation();
                valid = false;
            }
        }

        if (taxDetails.getLossOffsetCutOffMonth() != null
                && (taxDetails.getLossOffsetCutOffMonth() < 1 || taxDetails.getLossOffsetCutOffMonth() > 12)) {
            cvc.buildConstraintViolationWithTemplate("Loss offset month must be between 1-12.")
                    .addPropertyNode(MONTH_FIELD_NAME).addConstraintViolation();
            valid = false;
        }

        if (taxDetails.getLossOffsetCutOffDay() != null) {
            if (taxDetails.getLossOffsetCutOffDay() <= 0) {
                cvc.buildConstraintViolationWithTemplate("Loss offset days must be positive.")
                        .addPropertyNode(DAY_FIELD_NAME).addConstraintViolation();
                valid = false;
            }

            valid = isDayOfMonthValid(taxDetails, cvc) && valid;
        }
        return valid;
    }

    private boolean isDayOfMonthValid(TaxDetailsCommand taxDetails, ConstraintValidatorContext cvc) {
        boolean valid = true;
        Map<Integer, Integer> allowedDaysForMonth = new java.util.HashMap<>(Map.of(1, 31,
                2, 28,
                3, 31,
                4, 30,
                5, 31,
                6, 30,
                7, 31, 8, 31,
                9, 30,
                10, 31));
        allowedDaysForMonth.putAll(Map.of(11, 30, 12, 31));
        if (taxDetails.getLossOffsetCutOffMonth() == null) {
            cvc.buildConstraintViolationWithTemplate("If loss offset day is provided, loss offset month must also be provided.")
                    .addPropertyNode(MONTH_FIELD_NAME).addConstraintViolation();
            valid = false;
        } else if (allowedDaysForMonth.get(taxDetails.getLossOffsetCutOffMonth()) < taxDetails.getLossOffsetCutOffDay()) {
            cvc.buildConstraintViolationWithTemplate(taxDetails.getLossOffsetCutOffDay()
                            + " is not a valid day in month " + taxDetails.getLossOffsetCutOffMonth())
                    .addPropertyNode(DAY_FIELD_NAME).addConstraintViolation();
            valid = false;
        }
        return valid;
    }

    private boolean areCutOffDatesPresent(TaxDetailsCommand taxDetails, ConstraintValidatorContext cvc) {
        boolean valid = true;
        if (taxDetails.getLossOffsetCutOffMonth() == null) {
            cvc.buildConstraintViolationWithTemplate("If loss offset years are provided, loss offset cutoff date must be filled in.")
                    .addPropertyNode(MONTH_FIELD_NAME).addConstraintViolation();
            valid = false;
        }
        if (taxDetails.getLossOffsetCutOffDay() == null) {
            cvc.buildConstraintViolationWithTemplate("If loss offset years are provided, loss offset cutoff date must be filled in.")
                    .addPropertyNode(DAY_FIELD_NAME).addConstraintViolation();
            valid = false;
        }
        return valid;
    }

}
