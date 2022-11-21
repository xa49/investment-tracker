package app.analysis.liquid;

import app.analysis.CashValue;
import app.analysis.CashValueWithTaxDetails;
import app.analysis.actual.ActualPositionService;
import app.analysis.TaxCalculator;
import app.analysis.tracker.ActualPositionTracker;
import app.analysis.tracker.LiquidPositionTracker;
import app.broker.account.BrokerAccount;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class LiquidValueOrchestrator {

    private final CashFlowConverter cashFlowConverter;
    private final TaxCalculator taxCalculator;
    private final ActualPositionService actualPositionService;
    private final LiquidationDataConstructor liquidationDataConstructor;

    public LiquidPositionTracker getLiquidPositionTracker(
            List<Long> accountIds, String taxResidence, LocalDate asOfDate, String currency) {
        ActualPositionTracker actualPositionTracker =
                actualPositionService.getActualPositionTracker(accountIds, taxResidence, asOfDate);
        LiquidPositionTracker liquidPositionTracker =
                new LiquidPositionTracker(actualPositionTracker, asOfDate, currency);
        calculateLiquidValue(liquidPositionTracker, taxResidence);
        return liquidPositionTracker;
    }

    private void calculateLiquidValue(LiquidPositionTracker tracker, String taxResidence) {
        Map<BrokerAccount, BrokerAccount> mainAccounts =
                liquidationDataConstructor.getMainAccounts(tracker, tracker.getValueDate());
        tracker.mergeSecurityPositions(mainAccounts);

        closeSecurityPositions(tracker, taxResidence);

        tracker.mergeCashToMainAccount(mainAccounts);

        Map<BrokerAccount, List<CashValueWithTaxDetails>> transferFees =
                liquidationDataConstructor.getTransferFees(
                        tracker.getBrokerCashBalances(), tracker.getValueDate(), taxResidence);
        tracker.moveAllCashToBankAccount(transferFees);
        tracker.recordLiquidationTaxEffects(taxCalculator.calculateLiquidValueTaxEffect(tracker, taxResidence));

        CashValue liquidValue = convertCashToCommonCurrency(tracker.getBankBalances(), tracker);
        tracker.setLiquidValue(liquidValue);
    }

    private void closeSecurityPositions(LiquidPositionTracker tracker, String taxResidence) {
        tracker.getSecurityPositions().forEach(
                (account, holdings) -> holdings.forEach(
                        (security, positions) -> {
                            if(!positions.isEmpty()) {
                                tracker.recordExitEffects(
                                        liquidationDataConstructor.getMarketExitEffect( account, positions,
                                                tracker.getValueDate(), taxResidence));
                            }
                        }
                )
        );
    }

    private CashValue convertCashToCommonCurrency(Map<String, BigDecimal> balances, LiquidPositionTracker tracker) {
        return cashFlowConverter.convertAll(balances, tracker);
    }

}
