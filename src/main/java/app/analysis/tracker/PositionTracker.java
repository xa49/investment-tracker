package app.analysis.tracker;

import app.analysis.CashValue;
import app.analysis.DatedCashValue;
import app.analysis.actual.SecurityPosition;
import app.broker.account.BrokerAccount;
import app.data.securities.security.Security;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public abstract class PositionTracker {

    protected final CashTracker cashTracker;
    protected final SecurityTracker securityTracker;
    protected final TaxTracker taxTracker;

    protected PositionTracker(CashTracker cashTracker, SecurityTracker securityTracker, TaxTracker taxTracker) {
        this.cashTracker = cashTracker;
        this.securityTracker = securityTracker;
        this.taxTracker = taxTracker;
    }

    // CashTracker operations
    public Map<BrokerAccount, Map<String, BigDecimal>> getBrokerCashBalances() {
        return cashTracker.getBrokerBalances();
    }

    public List<DatedCashValue> getReturnCashFlows() {
        return cashTracker.getReturnCashFlows();
    }

    public Map<String, BigDecimal> getBankBalances() {
        return cashTracker.getBankBalances();
    }

    // SecurityTracker operations
    public Map<BrokerAccount, Map<Security, List<SecurityPosition>>> getSecurityPositions() {
        return securityTracker.getSecurityPositions();
    }

    // TaxTracker operations
    public CashValue getFeeWriteOffAvailable() {
        return taxTracker.getFeeWriteOffAvailable();
    }

    public List<DatedCashValue> getLossOffsetAvailable() {
        return taxTracker.getLossOffsetAvailable();
    }

    public String getTaxationCurrency() {
        return taxTracker.getTaxationCurrency();
    }

    public void changeTaxCurrency(String nextCurrency, BigDecimal exchangeRateToNextCurrency) {
        taxTracker.changeTaxationCurrency(nextCurrency, exchangeRateToNextCurrency);
    }

    // Shared operations
    public void processFee(BrokerAccount account, String currencyCode,
                           BigDecimal amount, CashValue feeAmountInTaxCurrency) {
        cashTracker.deductBrokerBalance(account, currencyCode, amount);
        taxTracker.addAvailableFeeWriteOff(feeAmountInTaxCurrency);
    }
}
