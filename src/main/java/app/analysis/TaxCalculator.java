package app.analysis;

import app.analysis.actual.SecurityPosition;
import app.analysis.actual.TaxEffectDto;
import app.analysis.liquid.IntermediateTaxEffectDto;
import app.analysis.tracker.ActualPositionTracker;
import app.analysis.tracker.LiquidPositionTracker;
import app.analysis.tracker.PositionTracker;
import app.broker.account.BrokerAccount;
import app.data.DataService;
import app.taxation.TbszCalculator;
import app.taxation.details.TaxDetails;
import app.taxation.details.TaxDetailsService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@AllArgsConstructor
public class TaxCalculator {

    private final TaxDetailsService taxDetailsService;
    private final DataService dataService;
    private final TbszCalculator tbszCalculator;

    public TaxDetails getTaxDetails(String taxResidence, LocalDate date) {
        return taxDetailsService.getTaxDetails(taxResidence, date);
    }

    public CashValue getCashInTaxCurrency(CashValue cashValue, LocalDate date, String taxResidence) {
        TaxDetails taxDetails = getTaxDetails(taxResidence, date);
        return new CashValue(cashValue.getAmount()
                .multiply(getExchangeRateToTaxCurrency(date, taxDetails, cashValue.getCurrency())),
                taxDetails.getTaxationCurrency());
    }

    public CashValue calculateTaxableGain(
            BrokerAccount account, CashValue proceeds, List<SecurityPosition> positionsClosed,
            LocalDate closeDate, TaxDetails taxDetails) {
        if (tbszCalculator.isEligible(account, closeDate)) {
            return tbszCalculator.calculateGains(account, closeDate, proceeds, positionsClosed, taxDetails);
        }

        return calculateRegularGain(proceeds, positionsClosed, closeDate, taxDetails);
    }

    public CashValue calculateRegularGain(
            CashValue proceeds, List<SecurityPosition> positionsClosed, LocalDate closeDate, TaxDetails taxDetails) {
        // If SecurityPositions are not all the same currency, there is a pro-rata allocation of the proceeds
        Map<String, BigDecimal> soldCountByPurchaseCurrency = new HashMap<>();
        Map<String, CashValue> costByPurchaseCurrency = new HashMap<>();

        addPositionsToMaps(positionsClosed, soldCountByPurchaseCurrency, costByPurchaseCurrency);

        BigDecimal totalCountSold = soldCountByPurchaseCurrency.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return soldCountByPurchaseCurrency.entrySet().stream()
                .reduce(CashValue.of(BigDecimal.ZERO, taxDetails.getTaxationCurrency()),
                        (acc, current) -> {
                            CashValue proceed =
                                    calculateProportionalProceed(current.getValue(), totalCountSold, proceeds);

                            BigDecimal exchangeToPurchaseCurrency =
                                    dataService.getExchangeRate(proceed.getCurrency(), current.getKey(), closeDate);
                            CashValue proceedInPurchaseCurrency =
                                    proceed.exchange(exchangeToPurchaseCurrency, current.getKey());
                            CashValue gainInPurchaseCurrency =
                                    proceedInPurchaseCurrency.subtract(costByPurchaseCurrency.get(current.getKey()));

                            return acc.add(gainInPurchaseCurrency
                                    .exchange(getExchangeRateToTaxCurrency(closeDate, taxDetails, current.getKey()),
                                            taxDetails.getTaxationCurrency()));
                        }, (oldAcc, newAcc) -> newAcc);
    }

    private void addPositionsToMaps(
            List<SecurityPosition> positionsClosed, Map<String, BigDecimal> soldCountByPurchaseCurrency,
            Map<String, CashValue> costByPurchaseCurrency) {
        positionsClosed.forEach(
                s -> {
                    String currency = s.getEnteredAt().getCurrency();
                    soldCountByPurchaseCurrency.put(currency,
                            soldCountByPurchaseCurrency.getOrDefault(currency, BigDecimal.ZERO)
                                    .add(s.getCount()));
                    costByPurchaseCurrency.put(currency,
                            costByPurchaseCurrency.getOrDefault(currency, CashValue.of(BigDecimal.ZERO, currency))
                                    .add(s.getEnteredAt().multiply(s.getCount())));
                }
        );
    }

    private CashValue calculateProportionalProceed(BigDecimal currentCount, BigDecimal totalCount, CashValue proceeds) {
        return proceeds.multiply(currentCount).divide(totalCount);
    }

    public TaxEffectDto getTaxEffect(ActualPositionTracker tracker, List<SecurityPosition> closedPositions,
                                     LocalDate date, BrokerAccount account, CashValue proceeds, String taxResidence) {
        TaxDetails taxDetails = getTaxDetails(taxResidence, date);
        verifyTaxationCurrencyCorrect(tracker, taxDetails);

        IntermediateTaxEffectDto intermediateTax = getIntermediateTaxEffect(date, account, proceeds, taxDetails,
                closedPositions, tracker.getFeeWriteOffAvailable(), tracker.getLossOffsetAvailable());

        CashValue tax = intermediateTax.getTaxableGain().multiply(
                taxDetails.getFlatCapitalGainsTaxRate()).multiply(new BigDecimal("0.01"));

        return TaxEffectDto.fromIntermediateDto(intermediateTax, tax);
    }

    public TaxEffectDto calculateLiquidValueTaxEffect(LiquidPositionTracker tracker, String taxResidence) {
        TaxDetails taxDetails = getTaxDetails(taxResidence, tracker.getValueDate());
        verifyTaxationCurrencyCorrect(tracker, taxDetails);

        List<DatedCashValue> lossOffsetUsed =
                getLossOffsetToUse(taxDetails, tracker.getValueDate(), tracker.getUntaxedGain(),
                        tracker.getLossOffsetAvailable());

        CashValue feeWriteOffUsed = tracker.getUntaxedGain().min(tracker.getFeeWriteOffAvailable().getAmount());
        CashValue tax = tracker.getUntaxedGain()
                .subtract(feeWriteOffUsed)
                .subtract(lossOffsetUsed)
                .multiply(taxDetails.getFlatCapitalGainsTaxRate().multiply(new BigDecimal("0.01")));

        return TaxEffectDto.builder()
                .taxPaid(tax)
                .lossAdded(new CashValue(BigDecimal.ZERO, taxDetails.getTaxationCurrency()))
                .lossesUsed(lossOffsetUsed)
                .feeUsed(feeWriteOffUsed)
                .transactionDate(tracker.getValueDate())
                .build();
    }


    private IntermediateTaxEffectDto getIntermediateTaxEffect(
            LocalDate exitDate, BrokerAccount account, CashValue proceeds, TaxDetails taxDetails,
            List<SecurityPosition> closedPositions, CashValue feeWriteOffAvailable,
            List<DatedCashValue> lossOffsetAvailable) {

        CashValue initialGain = calculateTaxableGain(account, proceeds, closedPositions, exitDate, taxDetails);
        List<DatedCashValue> lossOffsetToUse = getLossOffsetToUse(taxDetails, exitDate, initialGain, lossOffsetAvailable);
        CashValue gainAfterLossOffset = initialGain.subtract(lossOffsetToUse);
        CashValue feeToUse = gainAfterLossOffset.max(BigDecimal.ZERO).min(feeWriteOffAvailable);
        CashValue taxableGain = gainAfterLossOffset.subtract(feeToUse);

        return new IntermediateTaxEffectDto(
                taxableGain.max(BigDecimal.ZERO),
                taxableGain.min(BigDecimal.ZERO).abs(),
                lossOffsetToUse,
                feeToUse,
                exitDate
        );
    }

    public void verifyTaxationCurrencyCorrect(PositionTracker tracker, TaxDetails taxDetails) {
        if (tracker.getTaxationCurrency() == null) {
            tracker.changeTaxCurrency(taxDetails.getTaxationCurrency(), BigDecimal.ONE);
        } else if (!tracker.getTaxationCurrency().equals(taxDetails.getTaxationCurrency())) {
            String nextTaxationCurrency = taxDetails.getTaxationCurrency();
            tracker.changeTaxCurrency(nextTaxationCurrency,
                    dataService.getExchangeRate(
                            tracker.getTaxationCurrency(), nextTaxationCurrency, taxDetails.getFromDate()));
            // scenario when currency changed more than once inbetween not handled. (unrealistic that it occurs)
        }
    }

    private List<DatedCashValue> getLossOffsetToUse(TaxDetails taxDetails, LocalDate date, CashValue totalGains,
                                                    List<DatedCashValue> lossOffsetAvailable) {
        LocalDate cutOffDate = getLossOffsetCutOffDate(taxDetails, date);

        lossOffsetAvailable = new ArrayList<>(lossOffsetAvailable);
        lossOffsetAvailable.sort(Comparator.comparing(DatedCashValue::getDate));
        CashValue remainingGain = totalGains;

        List<DatedCashValue> lossesUsed = new ArrayList<>();
        Iterator<DatedCashValue> it = lossOffsetAvailable.iterator();
        while (it.hasNext() && remainingGain.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            DatedCashValue current = it.next();
            if (current.getDate().isBefore(cutOffDate)) {
                continue;
            }
            if (remainingGain.getAmount().compareTo(current.getAmount()) >= 0) {
                remainingGain = remainingGain.subtract(current);
                lossesUsed.add(current);
            } else {
                lossesUsed.add(new DatedCashValue(remainingGain, current.getDate()));
                remainingGain = new CashValue(BigDecimal.ZERO, null);
            }
        }
        return lossesUsed;
    }

    private LocalDate getLossOffsetCutOffDate(TaxDetails taxDetails, LocalDate date) {
        LocalDate onCutOffDayOfYear = date.withMonth(taxDetails.getLossOffsetCutOffMonth())
                .withDayOfMonth(taxDetails.getLossOffsetCutOffDay());
        if (onCutOffDayOfYear.isAfter(date)) {
            onCutOffDayOfYear = onCutOffDayOfYear.minusYears(1);
        }
        return onCutOffDayOfYear.minusYears(taxDetails.getLossOffsetYears());
    }

    private BigDecimal getExchangeRateToTaxCurrency(LocalDate date, TaxDetails taxDetails, String fromCurrency) {
        return dataService.getExchangeRate(fromCurrency, taxDetails.getTaxationCurrency(), date);
    }
}
