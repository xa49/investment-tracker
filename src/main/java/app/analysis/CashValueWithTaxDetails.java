package app.analysis;

import lombok.ToString;

import java.math.BigDecimal;
import java.util.Objects;

@ToString
public class CashValueWithTaxDetails extends CashValue {

    private final BigDecimal amountForTax;
    private final String currencyForTax;

    public static CashValueWithTaxDetails of(CashValue cashEffect, CashValue taxEffect) {
        return new CashValueWithTaxDetails(cashEffect.getAmount(), cashEffect.getCurrency(),
                taxEffect.getAmount(), taxEffect.getCurrency());
    }

    private CashValueWithTaxDetails(
            BigDecimal amount, String currency, BigDecimal amountForTax, String currencyForTax) {
        super(amount, currency);
        this.amountForTax = amountForTax;
        this.currencyForTax = currencyForTax;
    }

    public BigDecimal getAmountForTax() {
        return amountForTax;
    }

    public String getCurrencyForTax() {
        return currencyForTax;
    }

    public CashValue getCashValueForTax() {
        return CashValue.of(amountForTax, currencyForTax);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CashValueWithTaxDetails that = (CashValueWithTaxDetails) o;
        return Objects.equals(amountForTax, that.amountForTax) && Objects.equals(currencyForTax, that.currencyForTax);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), amountForTax, currencyForTax);
    }
}
