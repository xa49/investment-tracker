package app.taxation;

import app.analysis.CashValue;
import app.analysis.actual.SecurityPosition;
import app.broker.account.BrokerAccount;
import app.broker.account.BrokerAccountType;
import app.data.DataService;
import app.data.securities.price.SecurityPrice;
import app.manager.transaction.Transaction;
import app.manager.transaction.TransactionService;
import app.manager.transaction.TransactionType;
import app.taxation.details.TaxDetails;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class TbszCalculator {
    private static final MonthDay VAILD_TBSZ_EXIT_DAY = MonthDay.of(1, 1);
    private static final List<TransactionType> BREAKING_TRANSACTIONS =
            List.of(TransactionType.EXIT_INVESTMENT, TransactionType.TRANSFER_CASH, TransactionType.TRANSFER_SECURITY);
    private static final int TBSZ_COMMIT_YEARS = 5;

    private final DataService dataService;
    private final TransactionService transactionService;

    public boolean isEligible(BrokerAccount account, LocalDate currentDate) {
        LocalDate tbszThresholdDate = account.getOpenedDate()
                .withMonth(12).withDayOfMonth(31)
                .plusYears(TBSZ_COMMIT_YEARS);

        boolean completedFullCycle = isFullCycleCompleted(account, tbszThresholdDate);

        return account.getAccountType() == BrokerAccountType.TBSZ && tbszThresholdDate.isBefore(currentDate)
                && completedFullCycle;
    }

    public CashValue calculateGains(BrokerAccount account, LocalDate closeDate, CashValue proceeds,
                                    List<SecurityPosition> closedPositions, TaxDetails taxDetails) {
        CashValue eligibleCost = CashValue.of(BigDecimal.ZERO, taxDetails.getTaxationCurrency());
        LocalDate referenceDate = getTbszReferenceDate(account, closeDate);

        for (SecurityPosition p : closedPositions) {
            BigDecimal unitCost = getTbszCost(p, referenceDate, closeDate);
            eligibleCost = eligibleCost.add(unitCost.multiply(p.getCount()).multiply(dataService.getExchangeRate(
                    p.getEnteredAt().getCurrency(), taxDetails.getTaxationCurrency(), closeDate)));
        }

        return proceeds.exchange(
                        dataService.getExchangeRate(proceeds.getCurrency(), taxDetails.getTaxationCurrency(), closeDate),
                        taxDetails.getTaxationCurrency())
                .subtract(eligibleCost);
    }

    private BigDecimal getTbszCost(SecurityPosition position, LocalDate referenceDate, LocalDate closeDate) {
        SecurityPrice price = dataService.getSharePrice(position.getSecurity().getTicker(), referenceDate);
        BigDecimal currentPriceInTaxCurrency =
                price.getPrice().multiply(
                        dataService.getExchangeRate(price.getCurrency(), position.getEnteredAt().getCurrency(),
                                closeDate));
        return currentPriceInTaxCurrency.max(position.getEnteredAt().getAmount());
    }

    private boolean isFullCycleCompleted(BrokerAccount account, LocalDate tbszThresholdDate) {
        List<Transaction> accountExits =
                transactionService.getTransactionsOnTakeAccountByType(account.getId(), BREAKING_TRANSACTIONS);
        Optional<LocalDate> brokenDate = getFirstBrokenDate(accountExits);
        return brokenDate.isEmpty() || tbszThresholdDate.isBefore(brokenDate.get());
    }

    private Optional<LocalDate> getFirstBrokenDate(List<Transaction> accountExits) {
        return accountExits.stream()
                .filter(t -> !isValidExitDay(t.getTakeFromAccount(), t.getDate()))
                .min(Comparator.comparing(Transaction::getDate))
                .map(Transaction::getDate);
    }

    private boolean isValidExitDay(BrokerAccount account, LocalDate transactionDate) {
        if (!MonthDay.from(transactionDate).equals(VAILD_TBSZ_EXIT_DAY)) {
            return false;
        }
        return (transactionDate.getYear() - (account.getOpenedDate().getYear() + 1)) % TBSZ_COMMIT_YEARS == 0;
    }

    private LocalDate getTbszReferenceDate(BrokerAccount account, LocalDate currentDate) {
        List<Transaction> accountExits =
                transactionService.getTransactionsOnTakeAccountByType(account.getId(), BREAKING_TRANSACTIONS);

        return getFirstBrokenDate(accountExits)
                .orElse(getLastCycleCompleteDate(account, currentDate));
    }

    private LocalDate getLastCycleCompleteDate(BrokerAccount account, LocalDate currentDate) {
        LocalDate referenceDate = account.getOpenedDate().withMonth(12).withDayOfMonth(31);
        while (!referenceDate.plusYears(TBSZ_COMMIT_YEARS).isAfter(currentDate)) {
            referenceDate = referenceDate.plusYears(TBSZ_COMMIT_YEARS);
        }
        return referenceDate;
    }
}
