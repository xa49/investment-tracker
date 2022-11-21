package app.analysis.tracker;

import app.analysis.CashValue;
import app.analysis.DatedCashValue;
import app.broker.account.BrokerAccount;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class CashTracker {

    private Map<BrokerAccount, Map<String, BigDecimal>> brokerBalances = new HashMap<>();
    private List<DatedCashValue> returnCashFlows = new ArrayList<>();
    private final Map<String, BigDecimal> bankBalance = new HashMap<>();

    public CashTracker copy() {
        long startTime = System.nanoTime();

        Map<BrokerAccount, Map<String, BigDecimal>> cashBalancesCopy = new HashMap<>();
        for (Map.Entry<BrokerAccount, Map<String, BigDecimal>> entry : brokerBalances.entrySet()) {
            cashBalancesCopy.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }

        List<DatedCashValue> returnCashFlowCopy = new ArrayList<>();
        for (DatedCashValue cf : returnCashFlows) {
            returnCashFlowCopy.add(new DatedCashValue(cf.getAmount(), cf.getCurrency(), cf.getDate()));
        }

        log.info("Copied CashTracker with {} cash flows in {}ms",
                returnCashFlows.size(), (System.nanoTime() - startTime) / 1_000_000);
        return new CashTracker(cashBalancesCopy, returnCashFlowCopy);
    }

    public Map<BrokerAccount, Map<String, BigDecimal>> getBrokerBalances() {
        return Collections.unmodifiableMap(brokerBalances);
    }

    public List<DatedCashValue> getReturnCashFlows() {
        return Collections.unmodifiableList(returnCashFlows);
    }

    public void addBrokerCashBalance(BrokerAccount account, String currency, BigDecimal amount) {
        ensureBucketExists(account, currency);

        BigDecimal currentBalance = brokerBalances.get(account).get(currency);
        brokerBalances.get(account).put(currency, currentBalance.add(amount));
    }

    public void deductBrokerBalance(BrokerAccount account, String currency, BigDecimal amount) {
        addBrokerCashBalance(account, currency, amount.negate());
    }

    public void addReturnCashFlow(LocalDate date, String currency, BigDecimal amount) {
        returnCashFlows.add(new DatedCashValue(amount, currency, date));
    }

    public void addReturnCashFlow(DatedCashValue returnCashFlow) {
        returnCashFlows.add(returnCashFlow);
    }

    public void moveBrokerCashToBank() {
        brokerBalances.values().stream()
                .map(h -> h.entrySet().stream().toList())
                .flatMap(List::stream)
                .forEach(
                        es -> bankBalance.merge(es.getKey(), es.getValue(), BigDecimal::add)
                );
        brokerBalances.clear();
    }

    public void deductBankBalance(CashValue deduction) {
        bankBalance.put(deduction.getCurrency(),
                bankBalance.getOrDefault(deduction.getCurrency(), BigDecimal.ZERO).subtract(deduction.getAmount()));
    }

    public Map<String, BigDecimal> getBankBalances() {
        return Collections.unmodifiableMap(bankBalance);
    }

    public void mergeAccounts(BrokerAccount sourceAccount, BrokerAccount targetAccount) {
        if (!sourceAccount.equals(targetAccount) && brokerBalances.get(sourceAccount) != null) {
            brokerBalances.get(sourceAccount).forEach(
                    (currency, amount) -> {
                        ensureBucketExists(targetAccount, currency);
                        brokerBalances.get(targetAccount).merge(currency, amount, BigDecimal::add);
                    }
            );
            brokerBalances.remove(sourceAccount);
        }
    }

    private void ensureBucketExists(BrokerAccount account, String currency) {
        brokerBalances.putIfAbsent(account, new HashMap<>());
        brokerBalances.get(account).putIfAbsent(currency, BigDecimal.ZERO);
    }


}