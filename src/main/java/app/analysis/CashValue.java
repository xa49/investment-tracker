package app.analysis;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import java.util.Objects;

@Getter
@AllArgsConstructor
@ToString
public class CashValue {
    private BigDecimal amount;
    private String currency;

    public static CashValue of(BigDecimal amount, String  currency) {
        return new CashValue(amount, currency);
    }

    public static CashValue of(String amount, String currency) {
        return new CashValue(new BigDecimal(amount), currency);
    }

    public CashValue add(CashValue augend) {
        validate(augend);
        return new CashValue(this.amount.add(augend.amount), currency);
    }

    public CashValue add(BigDecimal augend) {
        return new CashValue(amount.add(augend), currency);
    }

    public CashValue subtract(CashValue subtrahend) {
        validate(subtrahend);
        return new CashValue(this.amount.subtract(subtrahend.amount), currency);
    }

    public CashValue subtract(List<? extends CashValue> cashValues) {
        return cashValues.stream()
                .reduce(this, (acc, current) -> {
                    validate(current);
                    return acc.subtract(current);
                }, (prev, actual) -> actual);
    }

    public CashValue negate() {
        return new CashValue(amount.negate(), currency);
    }

    public CashValue min(CashValue other) {
        validate(other);
        return new CashValue(amount.min(other.amount), currency);
    }

    public CashValue max(BigDecimal value) {
        if (value.compareTo(amount) > 0) {
            return new CashValue(value, currency);
        }
        return this;
    }

    public CashValue min(BigDecimal value) {
        if (value.compareTo(amount) < 0) {
            return new CashValue(value, currency);
        }
        return this;
    }

    public CashValue abs() {
        return this.max(amount.negate());
    }

    public CashValue multiply(BigDecimal amount) {
        return new CashValue(this.amount.multiply(amount), currency);
    }

    public CashValue exchange(BigDecimal exchangeRate, String newCurrency) {
        return CashValue.of(amount.multiply(exchangeRate), newCurrency);
    }

    @JsonIgnore
    public boolean isPresent() {
        return amount.compareTo(BigDecimal.ZERO) != 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CashValue that = (CashValue) o;
        return Objects.equals(amount, that.amount) && Objects.equals(currency, that.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }

    private void validate(CashValue other) {
        if(!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Operating on different currency CashValues: " + currency + ", "
                    + other.currency);
        }
    }

    public CashValue divide(BigDecimal divisor) {
        return CashValue.of(amount.divide(divisor, MathContext.DECIMAL64), currency);
    }

    public int compareTo(CashValue other) {
        validate(other);
        return amount.compareTo(other.amount);
    }
}
