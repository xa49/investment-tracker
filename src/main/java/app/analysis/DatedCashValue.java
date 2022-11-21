package app.analysis;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Getter
public class DatedCashValue extends CashValue {
    private final LocalDate date;

    public static DatedCashValue of(LocalDate date, BigDecimal amount, String currency) {
        return new DatedCashValue(CashValue.of(amount, currency), date);
    }

    public DatedCashValue(BigDecimal amount, String currency, LocalDate date) {
        super(amount, currency);
        this.date = date;
    }

    public DatedCashValue(CashValue cashFlow, LocalDate date) {
        this(cashFlow.getAmount(), cashFlow.getCurrency(), date);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DatedCashValue that = (DatedCashValue) o;
        return Objects.equals(date, that.date) && Objects.equals(this.getAmount(), that.getAmount())
                && Objects.equals(this.getCurrency(), that.getCurrency());
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, getAmount(), getCurrency());
    }
}
