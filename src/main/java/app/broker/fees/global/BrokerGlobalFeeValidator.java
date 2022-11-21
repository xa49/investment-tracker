package app.broker.fees.global;

import app.broker.BrokerEntityValidatorHelper;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.HashMap;
import java.util.Map;

public class BrokerGlobalFeeValidator implements ConstraintValidator<ValidGlobalFee, GlobalFeeCommand> {

    @Override
    public boolean isValid(GlobalFeeCommand brokerGlobalFee, ConstraintValidatorContext cvc) {
        cvc.disableDefaultConstraintViolation();
        boolean globalFixedFeeGroupValid = isGlobalFixedFeeGroupValid(brokerGlobalFee, cvc);
        boolean fixedFeeLimitValid = isFixedFeeLimitValid(brokerGlobalFee, cvc);
        boolean balanceFeeLimitValid = isBalanceFeeLimitValid(brokerGlobalFee, cvc);
        boolean validityDatesValid =
                BrokerEntityValidatorHelper.areValidityDatesCorrect(
                        brokerGlobalFee.getFromDate(), brokerGlobalFee.getToDate(), cvc);
        boolean referencePaymentDateValid = isReferencePaymentDateValid(brokerGlobalFee, cvc);

        return globalFixedFeeGroupValid && fixedFeeLimitValid && balanceFeeLimitValid
                && validityDatesValid && referencePaymentDateValid;
    }

    private boolean isReferencePaymentDateValid(GlobalFeeCommand brokerGlobalFee, ConstraintValidatorContext cvc) {
        if ((brokerGlobalFee.getGlobalFixedFeePeriod() != null || brokerGlobalFee.getGlobalFixedFeeAmt() != null
                || brokerGlobalFee.getGlobalFixedFeeCurrency() != null
                || brokerGlobalFee.getFixedFeeGlobalLimit() != null
                || brokerGlobalFee.getFixedFeeGlobalLimitPeriod() != null
                || brokerGlobalFee.getFixedFeeGlobalLimitCurrency() != null
                || brokerGlobalFee.getBalanceFeeGlobalLimit() != null
                || brokerGlobalFee.getBalanceFeeGlobalLimitPeriod() != null
                || brokerGlobalFee.getBalanceFeeGlobalLimitCurrency() != null)
                && brokerGlobalFee.getReferencePaymentDate() == null) {
            cvc.buildConstraintViolationWithTemplate("If any fee is present, a reference payment date must be provided.")
                    .addPropertyNode("referencePaymentDate").addConstraintViolation();
            return false;
        }
        return true;
    }

    private boolean isGlobalFixedFeeGroupValid(GlobalFeeCommand brokerGlobalFee, ConstraintValidatorContext cvc) {
        Map<String, Object> triple = new HashMap<>();
        triple.put("globalFixedFeeAmt", brokerGlobalFee.getGlobalFixedFeeAmt());
        triple.put("globalFixedFeeCurrency", brokerGlobalFee.getGlobalFixedFeeCurrency());
        triple.put("globalFixedFeePeriod", brokerGlobalFee.getGlobalFixedFeePeriod());
        return BrokerEntityValidatorHelper.isAllPresentInFeeGroup(triple, cvc);
    }

    private boolean isFixedFeeLimitValid(GlobalFeeCommand brokerGlobalFee, ConstraintValidatorContext cvc) {
        Map<String, Object> triple = new HashMap<>();
        triple.put("fixedFeeGlobalLimit", brokerGlobalFee.getFixedFeeGlobalLimit());
        triple.put("fixedFeeGlobalLimitCurrency", brokerGlobalFee.getFixedFeeGlobalLimitCurrency());
        triple.put("fixedFeeGlobalLimitPeriod", brokerGlobalFee.getFixedFeeGlobalLimitPeriod());
        return BrokerEntityValidatorHelper.isAllPresentInFeeGroup(triple, cvc);
    }

    private boolean isBalanceFeeLimitValid(GlobalFeeCommand brokerGlobalFee, ConstraintValidatorContext cvc) {
        Map<String, Object> triple = new HashMap<>();
        triple.put("balanceFeeGlobalLimit", brokerGlobalFee.getBalanceFeeGlobalLimit());
        triple.put("balanceFeeGlobalLimitCurrency", brokerGlobalFee.getBalanceFeeGlobalLimitCurrency());
        triple.put("balanceFeeGlobalLimitPeriod", brokerGlobalFee.getBalanceFeeGlobalLimitPeriod());
        return BrokerEntityValidatorHelper.isAllPresentInFeeGroup(triple, cvc);
    }
}
