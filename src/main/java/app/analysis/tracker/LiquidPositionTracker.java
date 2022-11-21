package app.analysis.tracker;

import app.analysis.CashValueWithTaxDetails;
import app.analysis.DatedCashValue;
import app.analysis.CashValue;
import app.analysis.actual.TaxEffectDto;
import app.analysis.liquid.LiquidExitEffectDto;
import app.analysis.liquid.LiquidValueDto;
import app.broker.account.BrokerAccount;
import app.broker.account.BrokerAccountType;
import app.util.InvalidDataException;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
public class LiquidPositionTracker extends PositionTracker {
    private final LocalDate valueDate;
    private final String liquidValueCurrency;
    private final LiquidValueDto liquidValueDto = new LiquidValueDto();
    private boolean liquidValueSet = false;

    public LiquidPositionTracker(PositionTracker positionTracker, LocalDate valueDate, String liquidValueCurrency) {
        super(positionTracker.cashTracker.copy(), positionTracker.securityTracker.copy(),
                new TaxTracker(positionTracker.taxTracker));

        this.valueDate = valueDate;
        this.liquidValueCurrency = liquidValueCurrency;

        addActualCashToDto();
    }

    public LiquidValueDto getLiquidValueDto() {
        return liquidValueDto;
    }

    public Map<String, BigDecimal> getBankBalances() {
        return cashTracker.getBankBalances();
    }

    public void mergeSecurityPositions(Map<BrokerAccount, BrokerAccount> mergingActions) {
        mergingActions.forEach(securityTracker::mergeAccounts);
        verifyOnlyMainAccountsRemain(securityTracker.getSecurityPositions());
    }

    public void recordExitEffects(LiquidExitEffectDto exitEffects) {
        cashTracker.addBrokerCashBalance(exitEffects.getAccount(), exitEffects.getGrossProceeds().getCurrency(),
                exitEffects.getGrossProceeds().getAmount());
        addSecurityMarketValue(exitEffects.getGrossProceeds());

        cashTracker.deductBrokerBalance(exitEffects.getAccount(), exitEffects.getFees().getCurrency(),
                exitEffects.getFees().getAmount().negate());
        taxTracker.addAvailableFeeWriteOff(exitEffects.getFees().getCashValueForTax().negate());
        addTransactionCommission(exitEffects.getFees().negate());

        taxTracker.addUntaxedGain(exitEffects.getUntaxedGain());
        taxTracker.addLossOffset(new DatedCashValue(exitEffects.getLossesAdded(), valueDate));
    }

    public void mergeCashToMainAccount(Map<BrokerAccount, BrokerAccount> mergingActions) {
        mergingActions.forEach(cashTracker::mergeAccounts);
        verifyOnlyMainAccountsRemain(cashTracker.getBrokerBalances());
    }

    public void moveAllCashToBankAccount(Map<BrokerAccount, List<CashValueWithTaxDetails>> transferFees) {
        deductTransferFeesFromBrokerBalance(transferFees);
        cashTracker.moveBrokerCashToBank();
    }

    public void recordLiquidationTaxEffects(TaxEffectDto taxEffects) {
        taxTracker.addTaxPayment(new DatedCashValue(taxEffects.getTaxPaid(), taxEffects.getTransactionDate()));
        cashTracker.deductBankBalance(taxEffects.getTaxPaid());
        taxTracker.removeUsedUpLosses(taxEffects.getLossesUsed());
        taxTracker.addLossOffset(DatedCashValue.of(valueDate, taxEffects.getLossAdded().getAmount(),
                taxEffects.getLossAdded().getCurrency()));
        taxTracker.useFeeWriteOff(taxEffects.getFeeUsed());
        liquidValueDto.setTaxDue(taxEffects.getTaxPaid());
    }

    public CashValue getUntaxedGain() {
        return taxTracker.getUntaxedGain();
    }

    public void setLiquidValue(CashValue liquidValue) {
        if (!liquidValue.getCurrency().equals(liquidValueCurrency)) {
            throw new InvalidDataException("Trying to set " + liquidValue.getCurrency()
                    + " as liquid value currency but request was " + liquidValueCurrency);
        }
        cashTracker.addReturnCashFlow(DatedCashValue.of(valueDate, liquidValue.getAmount(), liquidValue.getCurrency()));
        this.liquidValueSet = true;
        liquidValueDto.setFullyLiquidValue(liquidValue);
    }

    public void addActualCashToDto() {
        cashTracker.getBrokerBalances().forEach(
                (account, holdings) -> holdings.forEach(
                        (currency, amount) -> liquidValueDto.addActualCash(new CashValue(amount, currency))
                )
        );
    }

    public void compressContents() {
        liquidValueDto.compressLists();
    }

    public void setInvestmentReturnInPercent(Double returnInPercent) {
        liquidValueDto.setInvestmentReturnInPercent(returnInPercent);
    }

    private void verifyOnlyMainAccountsRemain(Map<BrokerAccount, ?> holdings) {
        holdings.keySet()
                .forEach(a -> {
                    if (a.getAccountType() != BrokerAccountType.MAIN) {
                        throw new IllegalStateException("After merging positions, non-main account remained: " + a);
                    }
                });
    }

    private void deductTransferFeesFromBrokerBalance(Map<BrokerAccount, List<CashValueWithTaxDetails>> transferFees) {
        transferFees.forEach(
                (account, fees) -> fees.forEach(
                        fee -> {
                            processFee(account, fee.getCurrency(), fee.getAmount().negate(), fee.getCashValueForTax());
                            liquidValueDto.addTransferFee(fee.negate());
                        }
                )
        );
    }

    private void addSecurityMarketValue(CashValue marketValue) {
        liquidValueDto.addSecurityMarketValue(marketValue);
    }

    private void addTransactionCommission(CashValue commission) {
        liquidValueDto.addTransactionCommission(commission);
    }
}
