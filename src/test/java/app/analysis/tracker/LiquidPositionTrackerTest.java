package app.analysis.tracker;

import app.analysis.CashValue;
import app.analysis.CashValueWithTaxDetails;
import app.analysis.DatedCashValue;
import app.analysis.actual.SecurityPosition;
import app.analysis.actual.TaxEffectDto;
import app.analysis.liquid.LiquidExitEffectDto;
import app.broker.account.BrokerAccount;
import app.broker.account.BrokerAccountType;
import app.data.securities.security.Security;
import app.util.InvalidDataException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LiquidPositionTrackerTest {

    LiquidPositionTracker liquidPositionTracker;
    ActualPositionTracker actualPositionTracker = ActualPositionTracker.getBlank();

    Security security = new Security("MMM", "3M Corporation", "NYSE", "USD");
    BrokerAccount sideAccount = new BrokerAccount();
    BrokerAccount mainAccount = new BrokerAccount();

    @BeforeEach
    void init() {
        sideAccount.setId(10L);
        sideAccount.setName("side");
        mainAccount.setId(9L);
        mainAccount.setName("main");
        mainAccount.setAccountType(BrokerAccountType.MAIN);

        liquidPositionTracker = new LiquidPositionTracker(actualPositionTracker, LocalDate.EPOCH, "EUR");
    }

    @Test
    void mergeAllPositionsToMainAccount() {
        actualPositionTracker.processEnterInvestment(LocalDate.EPOCH, sideAccount, security, BigDecimal.TEN, "EUR", BigDecimal.ONE);

        LiquidPositionTracker specialTracker = new LiquidPositionTracker(actualPositionTracker, LocalDate.EPOCH, "JPY");

        specialTracker.mergeSecurityPositions(Map.of(sideAccount, mainAccount));
        assertEquals(Map.of(mainAccount,
                        Map.of(security,
                                List.of(new SecurityPosition(security, BigDecimal.TEN, new CashValue(new BigDecimal("0.1"), "EUR"), LocalDate.EPOCH)))),
                specialTracker.getSecurityPositions());
    }

    @Test
    void mergeAllCashPositionsToMainAccount() {
        actualPositionTracker.processMoneyIn(LocalDate.EPOCH, sideAccount, "JPY", BigDecimal.TEN);
        LiquidPositionTracker specialTracker = new LiquidPositionTracker(actualPositionTracker, LocalDate.EPOCH, "GBP");

        specialTracker.mergeCashToMainAccount(Map.of(sideAccount, mainAccount));
        assertEquals(Map.of(mainAccount,
                        Map.of("JPY", BigDecimal.TEN)),
                specialTracker.getBrokerCashBalances());
    }


    @Test
    void mergeCash_leaveNonMainOpen() {
        actualPositionTracker.processMoneyIn(LocalDate.EPOCH, sideAccount, "JPY", BigDecimal.TEN);
        actualPositionTracker.processMoneyIn(LocalDate.EPOCH, mainAccount, "JPY", BigDecimal.TEN);
        LiquidPositionTracker specialTracker = new LiquidPositionTracker(actualPositionTracker, LocalDate.EPOCH, "GBP");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> specialTracker.mergeCashToMainAccount(Map.of(mainAccount, mainAccount)));
        assertEquals("After merging positions, non-main account remained: BrokerAccount{id=10, name='side', openedDate=null, closedDate=null, accountType=null}",
                ex.getMessage());


    }

    @Test
    void getValueDate() {
        assertEquals(LocalDate.EPOCH, liquidPositionTracker.getValueDate());
    }

    @Test
    void getLiquidValueCurrency() {
        assertEquals("EUR", liquidPositionTracker.getLiquidValueCurrency());
    }

    @Test
    void getBankAccountBalance() {
        assertEquals(Collections.emptyMap(), liquidPositionTracker.getBankBalances());

        liquidPositionTracker.cashTracker.addBrokerCashBalance(new BrokerAccount(), "JPY", BigDecimal.TEN);
        liquidPositionTracker.moveAllCashToBankAccount(Map.of(new BrokerAccount(), Collections.emptyList()));
        assertEquals(Map.of("JPY", new BigDecimal("10")), liquidPositionTracker.getBankBalances());
    }

    @Test
    void movingCashToBankAccount_feesAreAlsoRecorded() {
        liquidPositionTracker.changeTaxCurrency("HUF", BigDecimal.ONE);
        liquidPositionTracker.cashTracker.addBrokerCashBalance(mainAccount, "HUF", BigDecimal.TEN);
        liquidPositionTracker.cashTracker.addBrokerCashBalance(sideAccount, "EUR", new BigDecimal(20));

        liquidPositionTracker.moveAllCashToBankAccount(Map.of(
                mainAccount,
                List.of(CashValueWithTaxDetails.of(CashValue.of(BigDecimal.TEN.negate(), "EUR"), CashValue.of(BigDecimal.ONE, "HUF"))),
                sideAccount,
                List.of(CashValueWithTaxDetails.of(CashValue.of(BigDecimal.ONE.negate(), "EUR"), CashValue.of(BigDecimal.ONE, "HUF")))));

        assertEquals(Map.of("HUF", BigDecimal.TEN, "EUR", new BigDecimal(9)),
                liquidPositionTracker.getBankBalances());
        assertEquals(CashValue.of(new BigDecimal(2), "HUF"), liquidPositionTracker.getFeeWriteOffAvailable());
    }

    @Test
    void recordingExitEffects_recordsAllRelevantDetails() {
        LiquidExitEffectDto dto = LiquidExitEffectDto.builder()
                .account(new BrokerAccount())
                .grossProceeds(CashValue.of(BigDecimal.TEN, "EUR"))
                .fees(CashValueWithTaxDetails.of(
                        CashValue.of(BigDecimal.ONE.negate(), "USD"),
                        CashValue.of(BigDecimal.TEN.negate(), "HUF")))
                .untaxedGain(CashValue.of(new BigDecimal(2), "HUF"))
                .lossesAdded(CashValue.of(BigDecimal.ONE, "HUF"))
                .build();

        liquidPositionTracker.changeTaxCurrency("HUF", BigDecimal.ONE);
        liquidPositionTracker.recordExitEffects(dto);

        // Proceeds added to LiquidValueDto market value
        assertEquals(List.of(CashValue.of(BigDecimal.TEN, "EUR")),
                liquidPositionTracker.getLiquidValueDto().getMarketValueOfSecurities());
        // Proceeds added to broker balance & fees deducted from account
        assertEquals(Map.of("EUR", BigDecimal.TEN, "USD", BigDecimal.ONE.negate()),
                liquidPositionTracker.getBrokerCashBalances().get(new BrokerAccount()));
        // Fees added to LiquidValueDto commissions
        assertEquals(List.of(CashValue.of(BigDecimal.ONE, "USD")),
                liquidPositionTracker.getLiquidValueDto().getTransactionCommissions());
        // Fees added to fees available
        assertEquals(CashValue.of(BigDecimal.TEN, "HUF"), liquidPositionTracker.getFeeWriteOffAvailable());
        // Untaxed gain added
        assertEquals(CashValue.of(new BigDecimal(2), "HUF"), liquidPositionTracker.getUntaxedGain());
        // Losses added
        assertEquals(List.of(DatedCashValue.of(LocalDate.EPOCH, BigDecimal.ONE, "HUF")),
                liquidPositionTracker.getLossOffsetAvailable());
    }

    @Test
    void recordLiquidationTaxEffect_addsAllData() {
        TaxEffectDto dto = TaxEffectDto.builder()
                .transactionDate(LocalDate.EPOCH)
                .taxPaid(CashValue.of(BigDecimal.TEN, "HUF"))
                .feeUsed(CashValue.of(BigDecimal.ONE, "HUF"))
                .lossesUsed(List.of(DatedCashValue.of(LocalDate.EPOCH.plusDays(1), new BigDecimal(2), "HUF")))
                .lossAdded(CashValue.of(new BigDecimal(5), "HUF"))
                .build();

        liquidPositionTracker.changeTaxCurrency("HUF", BigDecimal.ONE);
        liquidPositionTracker.taxTracker.addLossOffset(DatedCashValue.of(LocalDate.EPOCH.plusDays(1), BigDecimal.TEN, "HUF"));
        liquidPositionTracker.taxTracker.addAvailableFeeWriteOff(CashValue.of(BigDecimal.TEN, "HUF"));

        liquidPositionTracker.recordLiquidationTaxEffects(dto);

        // Deducted from bank account
        assertEquals(Map.of("HUF", BigDecimal.TEN.negate()), liquidPositionTracker.getBankBalances());
        // Added tax to LiquidValueDto
        assertEquals(CashValue.of(BigDecimal.TEN, "HUF"), liquidPositionTracker.getLiquidValueDto().getTaxDue());
        // Deducted fee used
        assertEquals(CashValue.of(new BigDecimal(9), "HUF"), liquidPositionTracker.getFeeWriteOffAvailable());
        // Deducted & added loss offsets
        assertEquals(List.of(DatedCashValue.of(LocalDate.EPOCH.plusDays(1), new BigDecimal(8), "HUF"),
                        DatedCashValue.of(LocalDate.EPOCH, new BigDecimal(5), "HUF")),
                liquidPositionTracker.getLossOffsetAvailable());
    }

    @Test
    void addingInitialActualCashToLiquidValueDto() {
        liquidPositionTracker.cashTracker.addBrokerCashBalance(mainAccount, "EUR", BigDecimal.ONE);
        liquidPositionTracker.cashTracker.addBrokerCashBalance(mainAccount, "HUF", BigDecimal.ONE);
        liquidPositionTracker.cashTracker.addBrokerCashBalance(sideAccount, "EUR", BigDecimal.ONE);

        liquidPositionTracker.addActualCashToDto();
        liquidPositionTracker.compressContents();
        assertEquals(List.of(CashValue.of(new BigDecimal(2), "EUR"), CashValue.of(BigDecimal.ONE, "HUF")),
                liquidPositionTracker.getLiquidValueDto().getActualCashBalance());
    }

    @Test
    void compressContents() {
        liquidPositionTracker.cashTracker.addBrokerCashBalance(mainAccount, "EUR", BigDecimal.ONE);
        liquidPositionTracker.cashTracker.addBrokerCashBalance(mainAccount, "HUF", BigDecimal.ONE);
        liquidPositionTracker.cashTracker.addBrokerCashBalance(sideAccount, "EUR", BigDecimal.ONE);

        liquidPositionTracker.addActualCashToDto();
        liquidPositionTracker.compressContents();

        // Cash list compressed
        assertEquals(List.of(CashValue.of(new BigDecimal(2), "EUR"), CashValue.of(BigDecimal.ONE, "HUF")),
                liquidPositionTracker.getLiquidValueDto().getActualCashBalance());
        // other lists are not tested
    }

    @Test
    void settingLiquidValue() {
        liquidPositionTracker.setLiquidValue(CashValue.of(BigDecimal.TEN, "EUR"));

        // Added to dto
        assertEquals(CashValue.of(BigDecimal.TEN, "EUR"), liquidPositionTracker.getLiquidValueDto().getFullyLiquidValue());
        assertTrue(liquidPositionTracker.isLiquidValueSet());

        // Added to cash flow
        assertEquals(List.of(DatedCashValue.of(LocalDate.EPOCH, BigDecimal.TEN, "EUR")),
                liquidPositionTracker.getReturnCashFlows());
    }

    @Test
    void settingLiquidValue_notInRequestedCurrency() {
        InvalidDataException ex = assertThrows(InvalidDataException.class,
                () -> liquidPositionTracker.setLiquidValue(CashValue.of(BigDecimal.TEN, "HUF")));
        assertEquals("Trying to set HUF as liquid value currency but request was EUR", ex.getMessage());
    }

    @Test
    void setInvestmentReturnInPercent() {
        liquidPositionTracker.setInvestmentReturnInPercent(3.4);
        assertEquals(3.4, liquidPositionTracker.getLiquidValueDto().getInvestmentReturnInPercent());
    }
}