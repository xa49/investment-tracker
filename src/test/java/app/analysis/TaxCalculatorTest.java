package app.analysis;

import app.analysis.actual.SecurityPosition;
import app.analysis.actual.TaxEffectDto;
import app.analysis.tracker.*;
import app.broker.account.BrokerAccount;
import app.data.DataService;
import app.data.securities.security.Security;
import app.taxation.TbszCalculator;
import app.taxation.details.TaxDetails;
import app.taxation.details.TaxDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaxCalculatorTest {

    @Mock
    TaxDetailsService taxDetailsService;

    @Mock
    DataService dataService;

    @Mock
    TbszCalculator tbszCalculator;

    @InjectMocks
    TaxCalculator taxCalculator;

    TaxDetails taxDetailsHU = new TaxDetails();


    @BeforeEach
    void init() {
        taxDetailsHU.setTaxResidence("HU");
        taxDetailsHU.setTaxationCurrency("HUF");
        taxDetailsHU.setFlatCapitalGainsTaxRate(new BigDecimal(15));
    }

    @Test
    void getTaxDetails() {
        when(taxDetailsService.getTaxDetails("HU", LocalDate.EPOCH))
                .thenReturn(taxDetailsHU);

        TaxDetails queried = taxCalculator.getTaxDetails("HU", LocalDate.EPOCH);
        assertEquals("HUF", queried.getTaxationCurrency());
        assertEquals("HU", queried.getTaxResidence());
    }

    @Test
    void getCashInTaxCurrency() {
        when(taxDetailsService.getTaxDetails("HU", LocalDate.EPOCH))
                .thenReturn(taxDetailsHU);
        when(dataService.getExchangeRate("EUR", "HUF", LocalDate.EPOCH))
                .thenReturn(BigDecimal.TEN);

        assertEquals(CashValue.of(new BigDecimal(100), "HUF"),
                taxCalculator.getCashInTaxCurrency(CashValue.of(BigDecimal.TEN, "EUR"), LocalDate.EPOCH, "HU"));
    }

    @Test
    void calculateTaxableGain_notTbszEligible_allSameCurrency() { // todo not the same currencies, negative
        BrokerAccount account = new BrokerAccount();
        Security security = new Security("MMM", null, null, "USD");
        List<SecurityPosition> positionsClosed = List.of(
                new SecurityPosition(security, BigDecimal.ONE, CashValue.of(BigDecimal.ONE, "USD"), LocalDate.EPOCH.minusMonths(1)),
                new SecurityPosition(security, BigDecimal.TEN, CashValue.of(new BigDecimal(5), "USD"), LocalDate.EPOCH.minusDays(3))
        );
        when(tbszCalculator.isEligible(account, LocalDate.EPOCH))
                .thenReturn(false);
        when(dataService.getExchangeRate("USD", "HUF", LocalDate.EPOCH))
                .thenReturn(BigDecimal.TEN);
        when(dataService.getExchangeRate("USD", "USD", LocalDate.EPOCH))
                .thenReturn(BigDecimal.ONE);

        CashValue taxable = taxCalculator.calculateTaxableGain(account, CashValue.of(new BigDecimal(100), "USD"), positionsClosed,
                LocalDate.EPOCH, taxDetailsHU);

        // 100 - (1 x 1) - (10 x 5) = 49 USD -> x 10 = 490 HUF
        assertEquals(CashValue.of(new BigDecimal(490), "HUF"), taxable);
    }

    @Test
    void calculateTaxableGain_notTbszEligible_differentCurrencies() {
        BrokerAccount account = new BrokerAccount();
        Security security = new Security("MMM", null, null, "USD");
        List<SecurityPosition> positionsClosed = List.of(
                new SecurityPosition(security, BigDecimal.ONE, CashValue.of(BigDecimal.ONE, "HUF"), LocalDate.EPOCH.minusMonths(1)),
                new SecurityPosition(security, BigDecimal.ONE, CashValue.of(BigDecimal.ONE, "HUF"), LocalDate.EPOCH.minusMonths(1)),
                new SecurityPosition(security, BigDecimal.TEN, CashValue.of(new BigDecimal(5), "EUR"), LocalDate.EPOCH.minusDays(3))
        );
        when(tbszCalculator.isEligible(account, LocalDate.EPOCH))
                .thenReturn(false);
        when(dataService.getExchangeRate("USD", "HUF", LocalDate.EPOCH))
                .thenReturn(BigDecimal.TEN);
        when(dataService.getExchangeRate("HUF", "HUF", LocalDate.EPOCH))
                .thenReturn(BigDecimal.ONE);
        when(dataService.getExchangeRate("USD", "EUR", LocalDate.EPOCH))
                .thenReturn(new BigDecimal(2));
        when(dataService.getExchangeRate("EUR", "HUF", LocalDate.EPOCH))
                .thenReturn(new BigDecimal(3));


        CashValue taxable = taxCalculator.calculateTaxableGain(account, CashValue.of(new BigDecimal(120), "USD"), positionsClosed,
                LocalDate.EPOCH, taxDetailsHU);
        // 20 USD allocated to the 2 shares purchased in HUF. 20 USD × 10 = 200 HUF minus 2 x 1 cost = 198 HUF profit
        // 100 USD allocated to the 10 shares in EUR. 100 USD × 2 = 200 EUR minus 10 x 5 cost = 150 EUR profit x 3 = 450 HUF profit
        assertEquals(CashValue.of(new BigDecimal(648), "HUF"), taxable);

    }

    @Test
    void calculateTaxableGain_notTbszEligible_madeLoss() {
        BrokerAccount account = new BrokerAccount();
        Security security = new Security("MMM", null, null, "USD");
        List<SecurityPosition> positionsClosed = List.of(
                new SecurityPosition(security, BigDecimal.ONE, CashValue.of(BigDecimal.ONE, "HUF"), LocalDate.EPOCH.minusMonths(1)),
                new SecurityPosition(security, BigDecimal.ONE, CashValue.of(BigDecimal.ONE, "HUF"), LocalDate.EPOCH.minusMonths(1)),
                new SecurityPosition(security, BigDecimal.TEN, CashValue.of(new BigDecimal(5), "EUR"), LocalDate.EPOCH.minusDays(3))
        );
        when(tbszCalculator.isEligible(account, LocalDate.EPOCH))
                .thenReturn(false);
        when(dataService.getExchangeRate("USD", "HUF", LocalDate.EPOCH))
                .thenReturn(BigDecimal.TEN);
        when(dataService.getExchangeRate("HUF", "HUF", LocalDate.EPOCH))
                .thenReturn(BigDecimal.ONE);
        when(dataService.getExchangeRate("USD", "EUR", LocalDate.EPOCH))
                .thenReturn(new BigDecimal(2));
        when(dataService.getExchangeRate("EUR", "HUF", LocalDate.EPOCH))
                .thenReturn(new BigDecimal(3));


        CashValue taxable = taxCalculator.calculateTaxableGain(account, CashValue.of(new BigDecimal(12), "USD"), positionsClosed,
                LocalDate.EPOCH, taxDetailsHU);
        // 2 USD allocated to the 2 shares purchased in HUF. 2 USD × 10 = 20 HUF minus 2 x 1 cost = 18 HUF profit
        // 10 USD allocated to the 10 shares in EUR. 10 USD × 2 = 20 EUR minus 10 x 5 cost = -30 EUR profit x 3 = -90 HUF loss
        assertEquals(CashValue.of(new BigDecimal(-72), "HUF"), taxable);
    }

    @Test
    void calculateTaxableGain_tbszEligible() {
        BrokerAccount account = new BrokerAccount();
        Security security = new Security("MMM", null, null, "USD");
        List<SecurityPosition> positionsClosed = List.of(
                new SecurityPosition(security, BigDecimal.ONE, CashValue.of(BigDecimal.ONE, "HUF"), LocalDate.EPOCH.minusMonths(1)),
                new SecurityPosition(security, BigDecimal.ONE, CashValue.of(BigDecimal.ONE, "HUF"), LocalDate.EPOCH.minusMonths(1)),
                new SecurityPosition(security, BigDecimal.TEN, CashValue.of(new BigDecimal(5), "EUR"), LocalDate.EPOCH.minusDays(3))
        );
        when(tbszCalculator.isEligible(account, LocalDate.EPOCH))
                .thenReturn(true);
        when(tbszCalculator.calculateGains(account, LocalDate.EPOCH, CashValue.of(BigDecimal.TEN, "HUF"), positionsClosed, taxDetailsHU))
                .thenReturn(CashValue.of(BigDecimal.ONE, "EUR"));

        CashValue taxable = taxCalculator.calculateTaxableGain(account, CashValue.of(BigDecimal.TEN, "HUF"), positionsClosed, LocalDate.EPOCH, taxDetailsHU);
        assertEquals(CashValue.of(BigDecimal.ONE, "EUR"), taxable);
    }

    @Test
    void getTaxEffect_nothingToOffset() {
        BrokerAccount account = new BrokerAccount();
        Security security = new Security("MMM", null, null, "USD");
        List<SecurityPosition> positionsClosed = List.of(
                new SecurityPosition(security, BigDecimal.ONE, CashValue.of(BigDecimal.ONE, "HUF"), LocalDate.EPOCH.minusMonths(1)),
                new SecurityPosition(security, BigDecimal.ONE, CashValue.of(BigDecimal.ONE, "HUF"), LocalDate.EPOCH.minusMonths(1)),
                new SecurityPosition(security, BigDecimal.TEN, CashValue.of(new BigDecimal(5), "EUR"), LocalDate.EPOCH.minusDays(3))
        );
        when(taxDetailsService.getTaxDetails("HU", LocalDate.EPOCH))
                .thenReturn(taxDetailsHU);
        when(tbszCalculator.isEligible(account, LocalDate.EPOCH))
                .thenReturn(false);
        when(dataService.getExchangeRate("USD", "HUF", LocalDate.EPOCH))
                .thenReturn(BigDecimal.TEN);
        when(dataService.getExchangeRate("HUF", "HUF", LocalDate.EPOCH))
                .thenReturn(BigDecimal.ONE);
        when(dataService.getExchangeRate("USD", "EUR", LocalDate.EPOCH))
                .thenReturn(new BigDecimal(2));
        when(dataService.getExchangeRate("EUR", "HUF", LocalDate.EPOCH))
                .thenReturn(new BigDecimal(3));


        TaxEffectDto taxEffectDto = taxCalculator.getTaxEffect(ActualPositionTracker.getBlank(), positionsClosed,
                LocalDate.EPOCH, account, CashValue.of(new BigDecimal(120), "USD"), "HU");

        // The same 648 gain from calculateTaxableGain_notTbszEligible_differentCurrencies times 15% tax rate
        assertEquals(CashValue.of(new BigDecimal("97.20"), "HUF"), taxEffectDto.getTaxPaid());
        assertEquals(CashValue.of(BigDecimal.ZERO, "HUF"), taxEffectDto.getLossAdded());
        assertEquals(CashValue.of(BigDecimal.ZERO, "HUF"), taxEffectDto.getFeeUsed());
        assertEquals(Collections.emptyList(), taxEffectDto.getLossesUsed());
    }

    @Test
    void getTaxEffect_offsetFees() {
        BrokerAccount account = new BrokerAccount();
        Security security = new Security("MMM", null, null, "USD");
        List<SecurityPosition> positionsClosed = List.of(
                new SecurityPosition(security, BigDecimal.ONE, CashValue.of(BigDecimal.ONE, "HUF"), LocalDate.EPOCH.minusMonths(1)),
                new SecurityPosition(security, BigDecimal.ONE, CashValue.of(BigDecimal.ONE, "HUF"), LocalDate.EPOCH.minusMonths(1)),
                new SecurityPosition(security, BigDecimal.TEN, CashValue.of(new BigDecimal(5), "EUR"), LocalDate.EPOCH.minusDays(3))
        );

        ActualPositionTracker tracker = ActualPositionTracker.getBlank();
        tracker.changeTaxCurrency("HUF", BigDecimal.ONE);
        tracker.processFee(account, "HUF", BigDecimal.TEN, CashValue.of(BigDecimal.TEN, "HUF"));

        when(taxDetailsService.getTaxDetails("HU", LocalDate.EPOCH))
                .thenReturn(taxDetailsHU);
        when(tbszCalculator.isEligible(account, LocalDate.EPOCH))
                .thenReturn(false);
        when(dataService.getExchangeRate("USD", "HUF", LocalDate.EPOCH))
                .thenReturn(BigDecimal.TEN);
        when(dataService.getExchangeRate("HUF", "HUF", LocalDate.EPOCH))
                .thenReturn(BigDecimal.ONE);
        when(dataService.getExchangeRate("USD", "EUR", LocalDate.EPOCH))
                .thenReturn(new BigDecimal(2));
        when(dataService.getExchangeRate("EUR", "HUF", LocalDate.EPOCH))
                .thenReturn(new BigDecimal(3));

        TaxEffectDto taxEffectDto = taxCalculator.getTaxEffect(tracker, positionsClosed,
                LocalDate.EPOCH, account, CashValue.of(new BigDecimal(120), "USD"), "HU");

        // The same 648 gain from calculateTaxableGain_notTbszEligible_differentCurrencies less 10 fee offset times 15% tax rate
        assertEquals(CashValue.of(new BigDecimal("95.70"), "HUF"), taxEffectDto.getTaxPaid());
        assertEquals(CashValue.of(BigDecimal.ZERO, "HUF"), taxEffectDto.getLossAdded());
        assertEquals(CashValue.of(BigDecimal.TEN, "HUF"), taxEffectDto.getFeeUsed());
        assertEquals(Collections.emptyList(), taxEffectDto.getLossesUsed());
    }

    @Test
    void getTaxEffect_offsetFeesAndLossesBothUsed() {
        BrokerAccount account = new BrokerAccount();
        Security security = new Security("MMM", null, null, "USD");
        List<SecurityPosition> positionsClosed = List.of(
                new SecurityPosition(security, BigDecimal.ONE, CashValue.of(BigDecimal.ONE, "HUF"), LocalDate.EPOCH.minusMonths(1)),
                new SecurityPosition(security, BigDecimal.ONE, CashValue.of(BigDecimal.ONE, "HUF"), LocalDate.EPOCH.minusMonths(1)),
                new SecurityPosition(security, BigDecimal.TEN, CashValue.of(new BigDecimal(5), "EUR"), LocalDate.EPOCH.minusDays(3))
        );

        ActualPositionTracker tracker = ActualPositionTracker.getBlank();
        tracker.changeTaxCurrency("HUF", BigDecimal.ONE);
        tracker.processFee(account, "HUF", BigDecimal.TEN, CashValue.of(BigDecimal.TEN, "HUF"));
        tracker.processTax(TaxEffectDto.builder()
                .taxPaid(CashValue.of(BigDecimal.ZERO, "HUF"))
                .feeUsed(CashValue.of(BigDecimal.ZERO, "HUF"))
                .lossesUsed(Collections.emptyList())
                .lossAdded(CashValue.of(new BigDecimal(20), "HUF"))
                .transactionDate(LocalDate.EPOCH.minusMonths(10))
                .build());

        when(taxDetailsService.getTaxDetails("HU", LocalDate.EPOCH))
                .thenReturn(taxDetailsHU);
        when(tbszCalculator.isEligible(account, LocalDate.EPOCH))
                .thenReturn(false);
        when(dataService.getExchangeRate("USD", "HUF", LocalDate.EPOCH))
                .thenReturn(BigDecimal.TEN);
        when(dataService.getExchangeRate("HUF", "HUF", LocalDate.EPOCH))
                .thenReturn(BigDecimal.ONE);
        when(dataService.getExchangeRate("USD", "EUR", LocalDate.EPOCH))
                .thenReturn(new BigDecimal(2));
        when(dataService.getExchangeRate("EUR", "HUF", LocalDate.EPOCH))
                .thenReturn(new BigDecimal(3));

        TaxEffectDto taxEffectDto = taxCalculator.getTaxEffect(tracker, positionsClosed,
                LocalDate.EPOCH, account, CashValue.of(new BigDecimal(120), "USD"), "HU");

        // The same 648 gain from calculateTaxableGain_notTbszEligible_differentCurrencies less 10 fee offset less 20 loss times 15% tax rate
        assertEquals(CashValue.of(new BigDecimal("92.70"), "HUF"), taxEffectDto.getTaxPaid());
        assertEquals(CashValue.of(BigDecimal.ZERO, "HUF"), taxEffectDto.getLossAdded());
        assertEquals(CashValue.of(BigDecimal.TEN, "HUF"), taxEffectDto.getFeeUsed());
        assertEquals(List.of(DatedCashValue.of(LocalDate.EPOCH.minusMonths(10), new BigDecimal(20), "HUF")), taxEffectDto.getLossesUsed());
    }

    @Test
    void getTaxEffect_noGainAfterOffsets() {
        BrokerAccount account = new BrokerAccount();
        Security security = new Security("MMM", null, null, "USD");
        List<SecurityPosition> positionsClosed = List.of(
                new SecurityPosition(security, BigDecimal.ONE, CashValue.of(BigDecimal.ONE, "HUF"), LocalDate.EPOCH.minusMonths(1)),
                new SecurityPosition(security, BigDecimal.ONE, CashValue.of(BigDecimal.ONE, "HUF"), LocalDate.EPOCH.minusMonths(1)),
                new SecurityPosition(security, BigDecimal.TEN, CashValue.of(new BigDecimal(5), "EUR"), LocalDate.EPOCH.minusDays(3))
        );

        ActualPositionTracker tracker = ActualPositionTracker.getBlank();
        tracker.changeTaxCurrency("HUF", BigDecimal.ONE);
        tracker.processFee(account, "HUF", BigDecimal.TEN, CashValue.of(BigDecimal.TEN, "HUF"));
        tracker.processTax(TaxEffectDto.builder()
                .taxPaid(CashValue.of(BigDecimal.ZERO, "HUF"))
                .feeUsed(CashValue.of(BigDecimal.ZERO, "HUF"))
                .lossesUsed(Collections.emptyList())
                .lossAdded(CashValue.of(new BigDecimal(2000), "HUF"))
                .transactionDate(LocalDate.EPOCH.minusMonths(10))
                .build());

        when(taxDetailsService.getTaxDetails("HU", LocalDate.EPOCH))
                .thenReturn(taxDetailsHU);
        when(tbszCalculator.isEligible(account, LocalDate.EPOCH))
                .thenReturn(false);
        when(dataService.getExchangeRate("USD", "HUF", LocalDate.EPOCH))
                .thenReturn(BigDecimal.TEN);
        when(dataService.getExchangeRate("HUF", "HUF", LocalDate.EPOCH))
                .thenReturn(BigDecimal.ONE);
        when(dataService.getExchangeRate("USD", "EUR", LocalDate.EPOCH))
                .thenReturn(new BigDecimal(2));
        when(dataService.getExchangeRate("EUR", "HUF", LocalDate.EPOCH))
                .thenReturn(new BigDecimal(3));

        TaxEffectDto taxEffectDto = taxCalculator.getTaxEffect(tracker, positionsClosed,
                LocalDate.EPOCH, account, CashValue.of(new BigDecimal(120), "USD"), "HU");

        // The same 648 gain from calculateTaxableGain_notTbszEligible_differentCurrencies but all offset against losses which are first in order of use
        assertEquals(0, CashValue.of(BigDecimal.ZERO, "HUF").compareTo(taxEffectDto.getTaxPaid()));
        assertEquals(CashValue.of(BigDecimal.ZERO, "HUF"), taxEffectDto.getLossAdded());
        assertEquals(CashValue.of(BigDecimal.ZERO, "HUF"), taxEffectDto.getFeeUsed());
        assertEquals(List.of(DatedCashValue.of(LocalDate.EPOCH.minusMonths(10), new BigDecimal(648), "HUF")), taxEffectDto.getLossesUsed());

    }

    @Test
    void getTaxEffect_lossMade() {
        BrokerAccount account = new BrokerAccount();
        Security security = new Security("MMM", null, null, "USD");
        List<SecurityPosition> positionsClosed = List.of(
                new SecurityPosition(security, BigDecimal.ONE, CashValue.of(BigDecimal.ONE, "HUF"), LocalDate.EPOCH.minusMonths(1)),
                new SecurityPosition(security, BigDecimal.ONE, CashValue.of(BigDecimal.ONE, "HUF"), LocalDate.EPOCH.minusMonths(1)),
                new SecurityPosition(security, BigDecimal.TEN, CashValue.of(new BigDecimal(5), "EUR"), LocalDate.EPOCH.minusDays(3))
        );
        when(taxDetailsService.getTaxDetails("HU", LocalDate.EPOCH))
                .thenReturn(taxDetailsHU);
        when(tbszCalculator.isEligible(account, LocalDate.EPOCH))
                .thenReturn(false);
        when(dataService.getExchangeRate("USD", "HUF", LocalDate.EPOCH))
                .thenReturn(BigDecimal.TEN);
        when(dataService.getExchangeRate("HUF", "HUF", LocalDate.EPOCH))
                .thenReturn(BigDecimal.ONE);
        when(dataService.getExchangeRate("USD", "EUR", LocalDate.EPOCH))
                .thenReturn(new BigDecimal(2));
        when(dataService.getExchangeRate("EUR", "HUF", LocalDate.EPOCH))
                .thenReturn(new BigDecimal(3));


        TaxEffectDto taxEffectDto = taxCalculator.getTaxEffect(ActualPositionTracker.getBlank(), positionsClosed,
                LocalDate.EPOCH, account, CashValue.of(new BigDecimal(12), "USD"), "HU");

        // The same 72 loss from calculateTaxableGain_notTbszEligible_madeLoss
        assertEquals(0, CashValue.of(BigDecimal.ZERO, "HUF").compareTo(taxEffectDto.getTaxPaid()));
        assertEquals(CashValue.of(new BigDecimal("72"), "HUF"), taxEffectDto.getLossAdded());
        assertEquals(CashValue.of(BigDecimal.ZERO, "HUF"), taxEffectDto.getFeeUsed());
        assertEquals(Collections.emptyList(), taxEffectDto.getLossesUsed());
    }

    @Test
    void gettingLiquidValueTaxEffect() {
        TaxTracker taxTracker = new TaxTracker();
        taxTracker.changeTaxationCurrency("HUF", BigDecimal.ONE);
        taxTracker.addUntaxedGain(CashValue.of(new BigDecimal(100), "HUF"));
        taxTracker.addLossOffset(DatedCashValue.of(LocalDate.EPOCH.minusDays(1), BigDecimal.TEN, "HUF"));
        taxTracker.addAvailableFeeWriteOff(CashValue.of(BigDecimal.TEN, "HUF"));

        SecurityTracker securityTracker = new SecurityTracker();
        ActualPositionTracker actualPositionTracker = new ActualPositionTracker(new CashTracker(), securityTracker, taxTracker);
        LiquidPositionTracker tracker = new LiquidPositionTracker(actualPositionTracker, LocalDate.EPOCH, "EUR");

        when(taxDetailsService.getTaxDetails("HU", LocalDate.EPOCH))
                .thenReturn(taxDetailsHU);


        TaxEffectDto taxEffectDto = taxCalculator.calculateLiquidValueTaxEffect(tracker, "HU");
        assertEquals(0, CashValue.of(new BigDecimal("12"), "HUF").compareTo(taxEffectDto.getTaxPaid()));
        assertEquals(CashValue.of(BigDecimal.ZERO, "HUF"), taxEffectDto.getLossAdded());
        assertEquals(CashValue.of(BigDecimal.TEN, "HUF"), taxEffectDto.getFeeUsed());
        assertEquals(List.of(DatedCashValue.of(LocalDate.EPOCH.minusDays(1), BigDecimal.TEN, "HUF")), taxEffectDto.getLossesUsed());
    }
}