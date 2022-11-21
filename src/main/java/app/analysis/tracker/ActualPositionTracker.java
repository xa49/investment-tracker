package app.analysis.tracker;

import app.analysis.CashValue;
import app.analysis.DatedCashValue;
import app.analysis.actual.SecurityPosition;
import app.analysis.actual.TaxEffectDto;
import app.broker.account.BrokerAccount;
import app.data.securities.security.Security;
import app.manager.transaction.MatchingStrategy;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.util.List;

public class ActualPositionTracker extends PositionTracker {

    public ActualPositionTracker(CashTracker cashTracker, SecurityTracker securityTracker, TaxTracker taxTracker) {
        super(cashTracker, securityTracker, taxTracker);
    }

    public static ActualPositionTracker getBlank() {
        return new ActualPositionTracker(
                new CashTracker(),
                new SecurityTracker(),
                TaxTracker.getBlank());
    }

    public void processMoneyIn(LocalDate date, BrokerAccount addToAccount,
                               String currencyCode, BigDecimal amountAdded) {
        cashTracker.addBrokerCashBalance(addToAccount, currencyCode, amountAdded);
        cashTracker.addReturnCashFlow(date, currencyCode, amountAdded.negate());
    }

    public void processMoneyOut(LocalDate date, BrokerAccount takeFromAccount,
                                String currencyCode, BigDecimal amountTaken) {
        cashTracker.deductBrokerBalance(takeFromAccount, currencyCode, amountTaken);
        cashTracker.addReturnCashFlow(date, currencyCode, amountTaken);
    }

    public void processEnterInvestment(LocalDate date, BrokerAccount account, Security security,
                                       BigDecimal countPurchased,String currencyCode, BigDecimal totalPrice) {
        BigDecimal unitPrice = totalPrice.divide(countPurchased, MathContext.DECIMAL64);
        SecurityPosition position = new SecurityPosition(security, countPurchased,
                new CashValue(unitPrice, currencyCode), date);

        securityTracker.addPosition(account, position);
        processDeductMoneyFromAccount(account, currencyCode, totalPrice);
    }

    public List<SecurityPosition> processExitInvestment(BrokerAccount account, Security security, BigDecimal countSold,
                                                        CashValue totalProceeds, MatchingStrategy matchingStrategy) {
        processAddMoneyToAccount(account, totalProceeds.getCurrency(), totalProceeds.getAmount());
        return securityTracker.closePositions(account, security, countSold, matchingStrategy);
    }

    public void processTax(TaxEffectDto taxEffect) {
        if (taxEffect.getTaxPaid().isPresent()) {
            DatedCashValue taxPayment = new DatedCashValue(taxEffect.getTaxPaid(), taxEffect.getTransactionDate());
            taxTracker.addTaxPayment(taxPayment);
            cashTracker.deductBankBalance(taxPayment);
            cashTracker.addReturnCashFlow(new DatedCashValue(taxPayment.negate(), taxEffect.getTransactionDate()));
        }
        if (taxEffect.getLossAdded().isPresent()) {
            taxTracker.addLossOffset(new DatedCashValue(taxEffect.getLossAdded(), taxEffect.getTransactionDate()));
        }
        if (!taxEffect.getLossesUsed().isEmpty()) {
            taxTracker.removeUsedUpLosses(taxEffect.getLossesUsed());
        }
        if (taxEffect.getFeeUsed().isPresent()) {
            taxTracker.useFeeWriteOff(taxEffect.getFeeUsed());
        }
    }

    public void processTransferSecurity(BrokerAccount fromAccount, BrokerAccount toAccount, Security security,
                                        BigDecimal count, MatchingStrategy matchingStrategy) {
        securityTracker.transferPositions(fromAccount, toAccount, security, count, matchingStrategy);
    }

    public void processTransferCash(BrokerAccount fromAccount, BrokerAccount toAccount, String currencyCode,
                                    BigDecimal amount) {
        processAddMoneyToAccount(toAccount, currencyCode, amount);
        processDeductMoneyFromAccount(fromAccount, currencyCode, amount);
    }

    private void processAddMoneyToAccount(BrokerAccount addToAccount, String currencyCode,
                                          BigDecimal amountAdded) {
        cashTracker.addBrokerCashBalance(addToAccount, currencyCode, amountAdded);
    }

    private void processDeductMoneyFromAccount(BrokerAccount takeFromAccount, String currencyCode,
                                               BigDecimal amountTaken) {
        cashTracker.deductBrokerBalance(takeFromAccount, currencyCode, amountTaken);
    }
}
