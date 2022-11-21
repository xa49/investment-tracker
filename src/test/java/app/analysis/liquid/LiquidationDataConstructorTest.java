package app.analysis.liquid;

import app.analysis.CashValue;
import app.analysis.CashValueWithTaxDetails;
import app.analysis.TaxCalculator;
import app.analysis.actual.SecurityPosition;
import app.analysis.tracker.ActualPositionTracker;
import app.analysis.tracker.LiquidPositionTracker;
import app.broker.account.BrokerAccount;
import app.broker.account.BrokerAccountService;
import app.broker.fees.calculator.FeeCalculatorService;
import app.data.DataService;
import app.data.securities.price.SecurityPriceOfDay;
import app.data.securities.security.Security;
import app.taxation.details.TaxDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LiquidationDataConstructorTest {

    @Mock
    BrokerAccountService accountService;

    @Mock
    FeeCalculatorService feeCalculatorService;

    @Mock
    TaxCalculator taxCalculator;

    @Mock
    DataService dataService;

    @InjectMocks
    LiquidationDataConstructor liquidationDataConstructor;


    @Test
    void getMainAccounts() {
        BrokerAccount mainAccount = new BrokerAccount();
        mainAccount.setId(1L);
        BrokerAccount secondAccount = new BrokerAccount();
        secondAccount.setId(2L);
        BrokerAccount thirdAccount = new BrokerAccount();
        thirdAccount.setId(3L);

        Security security = new Security("MMM", null, null, "USD");

        ActualPositionTracker actualPositionTracker = ActualPositionTracker.getBlank();
        // Adding both cash and security accounts
        actualPositionTracker.processMoneyIn(LocalDate.EPOCH, mainAccount, "EUR", BigDecimal.TEN);
        actualPositionTracker.processMoneyIn(LocalDate.EPOCH, secondAccount, "EUR", BigDecimal.TEN);
        actualPositionTracker.processEnterInvestment(LocalDate.EPOCH, mainAccount, security, BigDecimal.TEN, "HUF", BigDecimal.TEN);
        actualPositionTracker.processEnterInvestment(LocalDate.EPOCH, thirdAccount, security, BigDecimal.TEN, "HUF", BigDecimal.TEN);

        LiquidPositionTracker tracker = new LiquidPositionTracker(actualPositionTracker, LocalDate.EPOCH, "HUF");

        when(accountService.getMainAccountForAccount(1L, LocalDate.EPOCH))
                .thenReturn(mainAccount);
        when(accountService.getMainAccountForAccount(2L, LocalDate.EPOCH))
                .thenReturn(secondAccount);
        when(accountService.getMainAccountForAccount(3L, LocalDate.EPOCH))
                .thenReturn(mainAccount);

        assertEquals(Map.of(mainAccount, mainAccount,
                secondAccount, secondAccount,
                thirdAccount, mainAccount), liquidationDataConstructor.getMainAccounts(tracker, LocalDate.EPOCH));
    }

    @Test
    void getTransferFees_onlyForPositiveBalances() {
        BrokerAccount mainAccount = new BrokerAccount();
        mainAccount.setId(1L);
        BrokerAccount secondAccount = new BrokerAccount();
        secondAccount.setId(2L);
        BrokerAccount thirdAccount = new BrokerAccount();
        thirdAccount.setId(3L);

        ActualPositionTracker actualPositionTracker = ActualPositionTracker.getBlank();
        actualPositionTracker.processMoneyIn(LocalDate.EPOCH, mainAccount, "EUR", BigDecimal.TEN);
        actualPositionTracker.processMoneyIn(LocalDate.EPOCH, mainAccount, "USD", BigDecimal.TEN);
        actualPositionTracker.processMoneyIn(LocalDate.EPOCH, secondAccount, "HUF", BigDecimal.TEN);
        // No transfer fee for negative balance
        actualPositionTracker.processMoneyIn(LocalDate.EPOCH, thirdAccount, "EUR", BigDecimal.TEN.negate());

        LiquidPositionTracker tracker = new LiquidPositionTracker(actualPositionTracker, LocalDate.EPOCH, "HUF");

        when(feeCalculatorService.getTransferFee(mainAccount.getId(), LocalDate.EPOCH, BigDecimal.TEN, "EUR"))
                .thenReturn(CashValue.of(BigDecimal.TEN, "HUF"));
        when(feeCalculatorService.getTransferFee(secondAccount.getId(), LocalDate.EPOCH, BigDecimal.TEN, "HUF"))
                .thenReturn(CashValue.of(BigDecimal.TEN, "HUF"));
        when(feeCalculatorService.getTransferFee(mainAccount.getId(), LocalDate.EPOCH, BigDecimal.TEN, "USD"))
                .thenReturn(CashValue.of(BigDecimal.TEN, "HUF"));
        when(taxCalculator.getCashInTaxCurrency(CashValue.of(BigDecimal.TEN.negate(), "HUF"), LocalDate.EPOCH, "HUF"))
                .thenReturn(CashValue.of(BigDecimal.ONE.negate(), "GBP"));


        Map<BrokerAccount, List<CashValueWithTaxDetails>> transferFees =
                liquidationDataConstructor.getTransferFees(tracker.getBrokerCashBalances(), LocalDate.EPOCH, "HUF");

        verify(feeCalculatorService, never()).getTransferFee(eq(thirdAccount.getId()), any(), any(), any());
        assertEquals(Map.of(
                        mainAccount, List.of(
                                CashValueWithTaxDetails.of(CashValue.of(BigDecimal.TEN, "HUF"), CashValue.of(BigDecimal.ONE.negate(), "GBP")),
                                CashValueWithTaxDetails.of(CashValue.of(BigDecimal.TEN, "HUF"), CashValue.of(BigDecimal.ONE.negate(), "GBP"))),
                        secondAccount, List.of(
                                CashValueWithTaxDetails.of(CashValue.of(BigDecimal.TEN, "HUF"), CashValue.of(BigDecimal.ONE.negate(), "GBP")))),
                transferFees);
    }

    @Test
    void getMarketExitEffect() {
        Security security = new Security("MMM", null, "NYSE", "USD");
        List<SecurityPosition> closedPositions = List.of(
                new SecurityPosition(security, BigDecimal.ONE, CashValue.of(BigDecimal.TEN, "EUR"), LocalDate.of(1900, 1, 1)),
                new SecurityPosition(security, BigDecimal.TEN, CashValue.of(BigDecimal.TEN, "USD"), LocalDate.of(1900, 1, 1))
        );

        when(dataService.getSharePrice("MMM", LocalDate.EPOCH))
                .thenReturn(new SecurityPriceOfDay(1L, "MMM", LocalDate.EPOCH, "USD", null, BigDecimal.TEN, null, null, null, 0));
        when(feeCalculatorService.getCommissionOnTransaction(null, LocalDate.EPOCH, "NYSE", new BigDecimal("110"), "USD"))
                .thenReturn(CashValue.of(BigDecimal.TEN, "GBP"));
        when(taxCalculator.getCashInTaxCurrency(CashValue.of(BigDecimal.TEN, "GBP"), LocalDate.EPOCH, "HU"))
                .thenReturn(CashValue.of(BigDecimal.ONE, "HUF"));
        when(taxCalculator.getTaxDetails("HU", LocalDate.EPOCH))
                .thenReturn(new TaxDetails());
        when(taxCalculator.calculateTaxableGain(new BrokerAccount(), CashValue.of(new BigDecimal("110"), "USD"), closedPositions, LocalDate.EPOCH, new TaxDetails()))
                .thenReturn(CashValue.of(new BigDecimal(5), "HUF"));

        LiquidExitEffectDto liquidExitEffectDto =
                liquidationDataConstructor.getMarketExitEffect(new BrokerAccount(), closedPositions, LocalDate.EPOCH, "HU");

        assertEquals(LiquidExitEffectDto.builder()
                        .account(new BrokerAccount())
                        .grossProceeds(CashValue.of(new BigDecimal("110"), "USD"))
                        .fees(CashValueWithTaxDetails.of(
                                CashValue.of(BigDecimal.TEN, "GBP"),
                                CashValue.of(BigDecimal.ONE, "HUF")))
                        .untaxedGain(CashValue.of(new BigDecimal(5), "HUF"))
                        .lossesAdded(CashValue.of(BigDecimal.ZERO, "HUF"))
                        .build()
                , liquidExitEffectDto);
    }
}