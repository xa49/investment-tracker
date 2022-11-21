package app.analysis.actual;

import app.analysis.portfolio.PortfolioService;
import app.analysis.tracker.ActualPositionTracker;
import app.manager.transaction.Transaction;
import app.manager.transaction.TransactionService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.Collator;
import java.time.LocalDate;
import java.util.*;

@Service
@AllArgsConstructor
public class ActualPositionService {

    private static final Comparator<? super String> HU_COLLATOR =
            Collator.getInstance(new Locale("hu", "HU"));
    private final PortfolioService portfolioService;
    private final TransactionService transactionService;
    private final TransactionProcessor transactionProcessor;


    public ActualPositionTracker getActualPositionTracker(
            List<Long> accountIds, String taxResidence, LocalDate asOfDate) {
        List<Transaction> transactions = transactionService.getTransactionsUntil(accountIds, asOfDate);
        return transactionProcessor.getNewPositionTracker(transactions, taxResidence);
    }

    public ActualPositionOverviewDto getActualPositionOverview(
            List<Long> accountIds, String taxResidence, LocalDate asOfDate) {
        ActualPositionTracker actualPositionTracker =
                getActualPositionTracker(accountIds, taxResidence, asOfDate);
        Map<String, Map<String , BigDecimal>> cashBalances =
                getCompactCashBalances(actualPositionTracker);
        Map<String , Map<String, BigDecimal>> securityBalances =
                getCompactSecurityBalances(actualPositionTracker);

        return ActualPositionOverviewDto.builder()
                .cashBalance(cashBalances)
                .securityBalance(securityBalances)
                .bankBalance(actualPositionTracker.getBankBalances())
                .feeWriteOffAvailable(actualPositionTracker.getFeeWriteOffAvailable())
                .lossOffsetsRecorded(actualPositionTracker.getLossOffsetAvailable())
                .build();
    }

    public ActualPositionOverviewDto getActualPositionOverview(
            String portfolioName, String taxResidence, LocalDate asOfDate) {
        Set<Long> accountIds = portfolioService.getAccountIdsInPortfolio(portfolioName);
        return getActualPositionOverview(new ArrayList<>(accountIds), taxResidence, asOfDate);
    }

    private Map<String, Map<String, BigDecimal>> getCompactCashBalances(ActualPositionTracker tracker) {
        Map<String, Map<String, BigDecimal>> cashBalances = new TreeMap<>(HU_COLLATOR);

        tracker.getBrokerCashBalances().forEach(
                ((account, holdings) -> {
                    cashBalances.put(account.getName(), new TreeMap<>(HU_COLLATOR));
                    holdings.forEach(
                            (currency, amount) -> cashBalances.get(account.getName()).put(currency, amount)
                    );
                })
        );
        return cashBalances;
    }

    private Map<String , Map<String , BigDecimal>> getCompactSecurityBalances(
            ActualPositionTracker tracker) {
        Map<String , Map<String , BigDecimal>> securityPositions = new TreeMap<>(HU_COLLATOR);

        tracker.getSecurityPositions().forEach(
                ((account, securityHoldings) -> {
                    securityPositions.put(account.getName(), new TreeMap<>(HU_COLLATOR));
                    securityHoldings.forEach(
                            (security, securityPositionList) -> {
                                BigDecimal securityCount = securityPositionList.stream()
                                        .map(SecurityPosition::getCount)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                securityPositions.get(account.getName())
                                        .put(security.getFullName(), securityCount);
                            }
                    );
                })
        );
        return securityPositions;
    }
}
