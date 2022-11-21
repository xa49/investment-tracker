package app.analysis;

import app.data.DataService;
import app.util.InvalidDataException;
import lombok.AllArgsConstructor;
import org.decampo.xirr.Transaction;
import org.decampo.xirr.Xirr;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@AllArgsConstructor
public class InvestmentArithmetic {

    private final DataService dataService;

    public double getLifetimeReturnInPercent(List<DatedCashValue> cashFlows, String currency) {
        List<Transaction> transactions = cashFlows.stream()
                .map(cf -> {
                    BigDecimal exchangedValue = cf.getAmount()
                            .multiply(dataService.getExchangeRate(cf.getCurrency(), currency, cf.getDate()));
                    return new Transaction(exchangedValue.doubleValue(), cf.getDate());
                })
                .toList();

        try {
            return new Xirr(transactions).xirr() * 100.0;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            throw new InvalidDataException("Return cannot be calculated");
        }
    }

}