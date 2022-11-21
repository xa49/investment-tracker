package app.analysis.liquid;

import app.analysis.CashValue;
import app.analysis.tracker.LiquidPositionTracker;
import app.data.DataService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Service
@AllArgsConstructor
public class CashFlowConverter {

    private static final BigDecimal REVOLUT_FEE_TILL_IMPLEMENTED = new BigDecimal("0.5");
    private final DataService dataService;

    public CashValue convertAll(Map<String, BigDecimal> balances, LiquidPositionTracker tracker) {
        return balances.entrySet().stream()
                .map(b -> CashValue.of(b.getValue(), b.getKey()))
                .map(c -> calculateTransferableNetAmount(c, tracker.getLiquidValueCurrency(), tracker.getValueDate(),
                        tracker.getLiquidValueDto()))
                .reduce(CashValue.of(BigDecimal.ZERO, tracker.getLiquidValueCurrency()), CashValue::add);
    }

    private CashValue calculateTransferableNetAmount(CashValue cashValue, String targetCurrency, LocalDate date,
                                                     LiquidValueDto liquidValueDto) {
        BigDecimal rate = dataService.getExchangeRate(
                cashValue.getCurrency(), targetCurrency, date);

        BigDecimal keepFactor = BigDecimal.ONE;
        if (!cashValue.getCurrency().equals(targetCurrency)
                && cashValue.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            liquidValueDto.getExchangeFees().add(
                    cashValue.multiply(REVOLUT_FEE_TILL_IMPLEMENTED.multiply(new BigDecimal("0.01")))
                            .exchange(rate, targetCurrency));

            keepFactor = BigDecimal.ONE.subtract(REVOLUT_FEE_TILL_IMPLEMENTED.multiply(new BigDecimal("0.01")));
        }
        return cashValue
                .exchange(rate, targetCurrency)
                .multiply(keepFactor);
    }
}
