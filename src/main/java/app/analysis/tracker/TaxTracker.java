package app.analysis.tracker;

import app.analysis.CashValue;
import app.analysis.DatedCashValue;
import app.util.InvalidDataException;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@NoArgsConstructor // used by ActualPositionTrackerTest - Mockito (for now)
public class TaxTracker {

    private String taxationCurrency;
    private final List<DatedCashValue> lossOffsetAvailable = new ArrayList<>();
    private BigDecimal feeWriteOffAvailable = BigDecimal.ZERO;
    private BigDecimal untaxedGain = BigDecimal.ZERO;

    private final List<DatedCashValue> taxesPaid = new ArrayList<>();

    public static TaxTracker getBlank() {
        return new TaxTracker((String) null);
    }

    public TaxTracker(TaxTracker tracker) {
        lossOffsetAvailable.addAll(tracker.lossOffsetAvailable.stream()
                .map(c -> new DatedCashValue(c.getAmount(), c.getCurrency(), c.getDate()))
                .toList());
        taxationCurrency = tracker.taxationCurrency;
        feeWriteOffAvailable = tracker.feeWriteOffAvailable;
        untaxedGain = tracker.untaxedGain;
        taxesPaid.addAll(tracker.taxesPaid.stream()
                .map(c -> new DatedCashValue(c.getAmount(), c.getCurrency(), c.getDate()))
                .toList());
    }

    private TaxTracker(String taxationCurrency) {
        this.taxationCurrency = taxationCurrency;
    }

    public List<DatedCashValue> getLossOffsetAvailable() {
        return Collections.unmodifiableList(lossOffsetAvailable);
    }

    public CashValue getFeeWriteOffAvailable() {
        return new CashValue(feeWriteOffAvailable, taxationCurrency);
    }

    public CashValue getUntaxedGain() {
        return new CashValue(untaxedGain, taxationCurrency);
    }

    public String getTaxationCurrency() {
        return taxationCurrency;
    }

    public List<DatedCashValue> getTaxesPaid() {
        return Collections.unmodifiableList(taxesPaid);
    }

    public void addLossOffset(DatedCashValue lossOffset) {
        verifyCurrencyMatches(lossOffset.getCurrency());
        lossOffsetAvailable.add(lossOffset);
    }

    public void addAvailableFeeWriteOff(CashValue feeAsPositive) {
        verifyCurrencyMatches(feeAsPositive.getCurrency());
        feeWriteOffAvailable = feeWriteOffAvailable.add(feeAsPositive.getAmount());
    }

    public void addUntaxedGain(CashValue additional) {
        verifyCurrencyMatches(additional.getCurrency());
        untaxedGain = untaxedGain.add(additional.getAmount());
    }

    public void useFeeWriteOff(CashValue usedAmount) {
        verifyCurrencyMatches(usedAmount.getCurrency());
        if (usedAmount.min(feeWriteOffAvailable).equals(usedAmount)) {
            feeWriteOffAvailable = feeWriteOffAvailable.subtract(usedAmount.getAmount());
        } else {
            throw new IllegalStateException("Not enough fee write off to deduct.");
        }
    }

    public void removeUsedUpLosses(List<DatedCashValue> removeItems) {
        List<DatedCashValue> partialOnes = new ArrayList<>(removeItems);
        partialOnes.removeAll(lossOffsetAvailable);
        // Remove the exactly matching cash flows (the fist instance of those)
        removeItems.forEach(lossOffsetAvailable::remove);
        // Remove the partial items
        for (DatedCashValue partial : partialOnes) {
            removePartialLossUse(partial);
        }
        // Don't leave zero values in list
        lossOffsetAvailable.removeIf(datedInvestorCashFlow -> datedInvestorCashFlow.getAmount().equals(BigDecimal.ZERO));
    }

    public void addTaxPayment(DatedCashValue taxPayment) {
        taxesPaid.add(taxPayment);
    }

    private void removePartialLossUse(DatedCashValue partial) {
        Iterator<DatedCashValue> it = lossOffsetAvailable.iterator();
        BigDecimal remainingToTake = partial.getAmount();
        List<DatedCashValue> remaindersToAddBack = new ArrayList<>();
        while (remainingToTake.compareTo(BigDecimal.ZERO) > 0 && it.hasNext()) {
            DatedCashValue current = it.next();
            if (current.getDate().equals(partial.getDate())) {
                BigDecimal removeAmount = remainingToTake.min(current.getAmount());
                remainingToTake = remainingToTake.subtract(removeAmount);
                remaindersToAddBack.add(DatedCashValue.of(current.getDate(), current.getAmount().subtract(removeAmount),
                        current.getCurrency()));
                it.remove();
            }
        }
        lossOffsetAvailable.addAll(remaindersToAddBack);
        if (remainingToTake.compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalStateException("Removed too much or too little loss offset. Remaining to remove: "
                    + remainingToTake);
        }
    }

    public void changeTaxationCurrency(String newTaxationCurrency, BigDecimal exchangeRateFromOldCurrency) {
        if (taxationCurrency != null && taxationCurrency.equals(newTaxationCurrency)) {
            return;
        }
        if (newTaxationCurrency == null) {
            throw new InvalidDataException("Cannot set taxation currency to null.");
        }
        if (exchangeRateFromOldCurrency == null || exchangeRateFromOldCurrency.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidDataException("Exchange rate to new currency must be positive.");
        }

        feeWriteOffAvailable = feeWriteOffAvailable.multiply(exchangeRateFromOldCurrency);
        untaxedGain = untaxedGain.multiply(exchangeRateFromOldCurrency);

        Iterator<DatedCashValue> it = lossOffsetAvailable.iterator();
        List<DatedCashValue> exchangedValues = new ArrayList<>();
        while (it.hasNext()) {
            DatedCashValue cf = it.next();
            exchangedValues.add(DatedCashValue.of(cf.getDate(), cf.getAmount().multiply(exchangeRateFromOldCurrency),
                    newTaxationCurrency));
            it.remove();
        }
        lossOffsetAvailable.addAll(exchangedValues);

        taxationCurrency = newTaxationCurrency;
    }

    private void verifyCurrencyMatches(String currency) {
        if (!currency.equals(taxationCurrency)) {
            throw new InvalidDataException("TaxTracker received " + currency
                    + " update request but it's taxation currency is: " + taxationCurrency);
        }
    }
}
