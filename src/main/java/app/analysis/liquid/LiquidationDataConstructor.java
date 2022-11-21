package app.analysis.liquid;

import app.analysis.CashValue;
import app.analysis.CashValueWithTaxDetails;
import app.analysis.actual.SecurityPosition;
import app.analysis.TaxCalculator;
import app.analysis.tracker.LiquidPositionTracker;
import app.broker.account.BrokerAccount;
import app.broker.account.BrokerAccountService;
import app.broker.fees.calculator.FeeCalculatorService;
import app.data.DataService;
import app.data.securities.price.SecurityPrice;
import app.data.securities.security.Security;
import app.taxation.details.TaxDetails;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@AllArgsConstructor
public class LiquidationDataConstructor {

    private final BrokerAccountService accountService;
    private final FeeCalculatorService feeCalculatorService;
    private final TaxCalculator taxCalculator;
    private final DataService dataService;

    public Map<BrokerAccount, BrokerAccount> getMainAccounts(
            LiquidPositionTracker liquidPositionTracker, LocalDate asOfDate) {
        return Stream.concat(
                        liquidPositionTracker.getSecurityPositions().keySet().stream(),
                        liquidPositionTracker.getBrokerCashBalances().keySet().stream())
                .collect(Collectors.toMap(
                        a -> a,
                        a -> accountService.getMainAccountForAccount(a.getId(), asOfDate),
                        (ex, nu) -> ex
                ));

    }

    public Map<BrokerAccount, List<CashValueWithTaxDetails>> getTransferFees(
            Map<BrokerAccount, Map<String, BigDecimal>> cashBalances, LocalDate date, String taxResidence) {
        Map<BrokerAccount, List<CashValueWithTaxDetails>> transferFees = new HashMap<>();

        for (Map.Entry<BrokerAccount, Map<String, BigDecimal>> brokerHolding : cashBalances.entrySet()) {
            brokerHolding.getValue().forEach(
                    (currency, amount) -> {
                        if (amount.compareTo(BigDecimal.ZERO) > 0) {
                            CashValue fees = feeCalculatorService.getTransferFee(brokerHolding.getKey().getId(), date,
                                    amount, currency);
                            CashValue taxEffect = taxCalculator.getCashInTaxCurrency(fees.negate(), date,
                                    taxResidence);
                            transferFees.computeIfAbsent(brokerHolding.getKey(), l -> new ArrayList<>())
                                    .add(CashValueWithTaxDetails.of(fees, taxEffect));
                        }
                    }
            );
        }
        return transferFees;
    }

    public LiquidExitEffectDto getMarketExitEffect(BrokerAccount account, List<SecurityPosition> securityPositions,
                                                   LocalDate date, String taxResidence) {
        if (securityPositions.isEmpty()) {
            throw new IllegalStateException("Empty position list to close");
        }

        Security security = securityPositions.get(0).getSecurity();
        CashValue proceeds = getProceeds(securityPositions, date, security);
        CashValue commissions = feeCalculatorService.getCommissionOnTransaction(
                account.getId(), date, security.getMarket(), proceeds.getAmount(), proceeds.getCurrency());
        TaxDetails taxDetails = taxCalculator.getTaxDetails(taxResidence, date);
        CashValue taxableGain =
                taxCalculator.calculateTaxableGain(account, proceeds, securityPositions, date, taxDetails);

        return LiquidExitEffectDto.builder()
                .account(account)
                .grossProceeds(proceeds)
                .fees(CashValueWithTaxDetails.of(
                        commissions,
                        taxCalculator.getCashInTaxCurrency(commissions, date, taxResidence)))
                .untaxedGain(taxableGain.max(BigDecimal.ZERO))
                .lossesAdded(taxableGain.min(BigDecimal.ZERO).abs())
                .build();
    }

    private CashValue getProceeds(List<SecurityPosition> securityPositions, LocalDate date, Security security) {
        BigDecimal countSold = securityPositions.stream()
                .reduce(BigDecimal.ZERO, (acc, pos) -> acc.add(pos.getCount()), (old, next) -> next);

        SecurityPrice sharePrice = dataService.getSharePrice(security.getTicker(), date);
        return new CashValue(countSold.multiply(sharePrice.getPrice()), sharePrice.getCurrency());
    }
}
