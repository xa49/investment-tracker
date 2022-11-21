package app.analysis.liquid;

import app.analysis.CashValue;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Setter
public class LiquidValueDto {
    private final List<CashValue> marketValueOfSecurities = new ArrayList<>();
    private final List<CashValue> actualCashBalance = new ArrayList<>();
    private final List<CashValue> transactionCommissions = new ArrayList<>();
    private final List<CashValue> transferFees = new ArrayList<>();
    private final List<CashValue> exchangeFees = new ArrayList<>();
    private CashValue taxDue;
    private CashValue fullyLiquidValue;
    private Double investmentReturnInPercent;


    public void addSecurityMarketValue(CashValue marketValue) {
        marketValueOfSecurities.add(marketValue);
    }

    public void addTransactionCommission(CashValue commission) {
        transactionCommissions.add(commission);
    }

    public void addActualCash(CashValue cashValue) {
        actualCashBalance.add(cashValue);
    }

    public void addTransferFee(CashValue fee) {
        transferFees.add(fee);
    }

    public void compressLists() {
        compressList(marketValueOfSecurities);
        compressList(actualCashBalance);
        compressList(transactionCommissions);
        compressList(transferFees);
        compressList(exchangeFees);
    }

    private void compressList(List<CashValue> list) {
        Map<String, CashValue> balances = list.stream()
                .collect(Collectors.toMap(
                        CashValue::getCurrency,
                        c -> c,
                        CashValue::add
                ));

        list.clear();
        list.addAll(balances.values());
    }
}
