package app.analysis.actual;

import app.analysis.CashValue;
import app.data.securities.security.Security;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class SecurityPosition {
    private Security security;
    private BigDecimal count;
    private CashValue enteredAt;
    private LocalDate enterDate;
}
