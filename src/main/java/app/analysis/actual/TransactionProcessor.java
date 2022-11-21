package app.analysis.actual;

import app.analysis.CashValue;
import app.analysis.TaxCalculator;
import app.analysis.tracker.ActualPositionTracker;
import app.broker.account.BrokerAccount;
import app.data.DataService;
import app.data.fx.currency.BasicCurrency;
import app.data.securities.security.Security;
import app.manager.transaction.Transaction;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class TransactionProcessor {

    private final DataService dataService;
    private final TaxCalculator taxCalculator;

    public ActualPositionTracker getNewPositionTracker(
            List<Transaction> transactions, String taxResidence) {
        ActualPositionTracker tracker = ActualPositionTracker.getBlank();
        addTransactionsToTracker(tracker, transactions, taxResidence);
        return tracker;
    }

    public void addTransactionsToTracker(
            ActualPositionTracker tracker, List<Transaction> transactions, String taxResidence) {
        Map<Long, String> currencyMapping = getIsoCodeByCurrencyId(transactions);
        Map<Long, Security> securityMapping = getSecuritiesByIds(transactions);

        for (Transaction transaction : transactions) {
            switch (transaction.getTransactionType()) {
                case MONEY_IN -> addMoneyInToTracker(transaction, tracker, currencyMapping);
                case MONEY_OUT -> addMoneyOutToTracker(transaction, tracker, currencyMapping);
                case ENTER_INVESTMENT ->
                        addEnterInvestmentToTracker(transaction, tracker, currencyMapping, securityMapping);
                case EXIT_INVESTMENT ->
                        addExitInvestmentToTracker(transaction, tracker, currencyMapping, securityMapping, taxResidence);
                case TRANSFER_SECURITY -> addTransferSecurityToTracker(transaction, tracker, securityMapping);
                case TRANSFER_CASH -> addTransferCashToTracker(transaction, tracker, currencyMapping);
                case PAY_FEE -> addPayFeeToTracker(transaction, tracker, currencyMapping, taxResidence);
                default -> throw new IllegalStateException(
                        "TransactionType is not implemented: " + transaction.getTransactionType());
            }
        }
    }

    private void addMoneyInToTracker(Transaction transaction, ActualPositionTracker tracker,
                                     Map<Long, String> currencyMapping) {
        BrokerAccount addToAccount = transaction.getAddToAccount();
        String currencyCode = currencyMapping.get(transaction.getAssetAdded().getAssetId());
        BigDecimal amountAdded = transaction.getCountOfAssetAdded();

        tracker.processMoneyIn(transaction.getDate(), addToAccount, currencyCode, amountAdded);
    }

    private void addMoneyOutToTracker(Transaction transaction, ActualPositionTracker tracker,
                                      Map<Long, String> currencyMapping) {
        BrokerAccount takeFromAccount = transaction.getTakeFromAccount();
        String currencyCode = currencyMapping.get(transaction.getAssetTaken().getAssetId());
        BigDecimal amountTaken = transaction.getCountOfAssetTaken();

        tracker.processMoneyOut(transaction.getDate(), takeFromAccount, currencyCode, amountTaken);
    }

    private void addEnterInvestmentToTracker(Transaction transaction, ActualPositionTracker tracker,
                                             Map<Long, String> currencyMapping, Map<Long, Security> securityMapping) {
        BrokerAccount account = transaction.getTakeFromAccount();
        String currencyCode = currencyMapping.get(transaction.getAssetTaken().getAssetId());
        BigDecimal totalPrice = transaction.getCountOfAssetTaken();
        Security security = securityMapping.get(transaction.getAssetAdded().getAssetId());
        BigDecimal countPurchased = transaction.getCountOfAssetAdded();

        tracker.processEnterInvestment(transaction.getDate(), account, security,
                countPurchased, currencyCode, totalPrice);
    }

    private void addExitInvestmentToTracker(Transaction transaction, ActualPositionTracker tracker,
                                            Map<Long, String> currencyMapping, Map<Long, Security> securityMapping,
                                            String taxResidence) {
        BrokerAccount account = transaction.getTakeFromAccount();
        String currencyCode = currencyMapping.get(transaction.getAssetAdded().getAssetId());
        CashValue totalProceeds = CashValue.of(transaction.getCountOfAssetAdded(), currencyCode);
        Security security = securityMapping.get(transaction.getAssetTaken().getAssetId());
        BigDecimal countSold = transaction.getCountOfAssetTaken();

        List<SecurityPosition> closedPositions = tracker.processExitInvestment(account, security, countSold,
                totalProceeds, transaction.getMatchingStrategy());
        TaxEffectDto taxEffect = taxCalculator.getTaxEffect(tracker, closedPositions,
                transaction.getDate(),account, totalProceeds, taxResidence);

        tracker.processTax(taxEffect);
    }

    private void addTransferSecurityToTracker(Transaction transaction, ActualPositionTracker tracker,
                                              Map<Long, Security> securityMapping) {
        BrokerAccount fromAccount = transaction.getTakeFromAccount();
        BrokerAccount toAccount = transaction.getAddToAccount();
        Security security = securityMapping.get(transaction.getAssetTaken().getAssetId());
        BigDecimal count = transaction.getCountOfAssetAdded();

        tracker.processTransferSecurity(fromAccount, toAccount, security, count, transaction.getMatchingStrategy());
    }

    private void addTransferCashToTracker(Transaction transaction, ActualPositionTracker tracker,
                                          Map<Long, String> currencyMapping) {
        BrokerAccount fromAccount = transaction.getTakeFromAccount();
        BrokerAccount toAccount = transaction.getAddToAccount();
        String currencyCode = currencyMapping.get(transaction.getAssetTaken().getAssetId());
        BigDecimal amount = transaction.getCountOfAssetAdded();

        tracker.processTransferCash(fromAccount, toAccount, currencyCode, amount);
    }

    private void addPayFeeToTracker(Transaction transaction, ActualPositionTracker tracker,
                                    Map<Long, String> currencyMapping, String taxResidence) {
        BrokerAccount account = transaction.getTakeFromAccount();
        String currencyCode = currencyMapping.get(transaction.getAssetTaken().getAssetId());
        BigDecimal amount = transaction.getCountOfAssetTaken();

        CashValue feeInTaxCurrency = taxCalculator.getCashInTaxCurrency(CashValue.of(amount, currencyCode),
                transaction.getDate(), taxResidence);

        taxCalculator.verifyTaxationCurrencyCorrect(
                tracker, taxCalculator.getTaxDetails(taxResidence, transaction.getDate()));
        tracker.processFee(account, currencyCode, amount, feeInTaxCurrency);
    }

    private Map<Long, Security> getSecuritiesByIds(List<Transaction> transactions) {
        List<Long> requiredIds = transactions.stream()
                .map(this::getSecurityIdsInTransaction)
                .flatMap(List::stream)
                .distinct()
                .toList();

        return dataService.getSecurityDetailsByIdList(requiredIds).stream()
                .collect(Collectors.toMap(
                        Security::getId,
                        s -> s
                ));
    }

    private Map<Long, String> getIsoCodeByCurrencyId(List<Transaction> transactions) {
        List<Long> requiredIds = transactions.stream()
                .map(this::getCurrencyIdsInTransaction)
                .flatMap(List::stream)
                .distinct()
                .toList();

        return dataService.getCurrencyDetailsByIdList(requiredIds).stream()
                .collect(Collectors.toMap(
                        BasicCurrency::getId,
                        BasicCurrency::getIsoCode
                ));
    }

    private List<Long> getCurrencyIdsInTransaction(Transaction t) {
        return switch (t.getTransactionType()) {
            case MONEY_OUT, PAY_FEE, ENTER_INVESTMENT -> List.of(t.getAssetTaken().getAssetId());
            case MONEY_IN, EXIT_INVESTMENT -> List.of(t.getAssetAdded().getAssetId());
            case TRANSFER_CASH -> List.of(t.getAssetTaken().getAssetId(), t.getAssetAdded().getAssetId());
            case TRANSFER_SECURITY -> Collections.emptyList();
        };
    }

    private List<Long> getSecurityIdsInTransaction(Transaction t) {
        return switch (t.getTransactionType()) {
            case EXIT_INVESTMENT -> List.of(t.getAssetTaken().getAssetId());
            case ENTER_INVESTMENT -> List.of(t.getAssetAdded().getAssetId());
            case TRANSFER_SECURITY -> List.of(t.getAssetTaken().getAssetId(), t.getAssetAdded().getAssetId());
            case MONEY_IN, MONEY_OUT, TRANSFER_CASH, PAY_FEE -> Collections.emptyList();
        };
    }

}
