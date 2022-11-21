package app.analysis.actual;

import app.analysis.CashValue;
import app.analysis.TaxCalculator;
import app.analysis.tracker.ActualPositionTracker;
import app.broker.account.BrokerAccount;
import app.data.DataService;
import app.data.fx.currency.BasicCurrency;
import app.data.securities.security.Security;
import app.manager.transaction.MatchingStrategy;
import app.manager.transaction.Transaction;
import app.manager.transaction.TransactionType;
import app.manager.transaction.asset_record.InvestmentAssetRecord;
import app.manager.transaction.asset_record.InvestmentAssetType;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionProcessorTest {

    @Mock
    DataService dataService;

    @Mock
    TaxCalculator taxCalculator;

    @InjectMocks
    TransactionProcessor transactionProcessor;

    BasicCurrency euro = new BasicCurrency("EUR", "euro");
    Security security = new Security("MMM", "3M Corporation", "NYSE", "USD");
    BrokerAccount account = new BrokerAccount();
    BrokerAccount anotherAccount = new BrokerAccount();

    @BeforeEach
    void init() {
        euro.setId(10L);
        security.setId(20L);
        account.setName("account");
        anotherAccount.setId(10L);

        lenient().when(dataService.getCurrencyDetailsByIdList(List.of(10L)))
                .thenReturn(List.of(euro));

        lenient().when(dataService.getSecurityDetailsByIdList(List.of(20L)))
                .thenReturn(List.of(security));
    }


    @Test
    void getNewPositionTracker_blank() {
        ActualPositionTracker tracker = transactionProcessor.getNewPositionTracker(Collections.emptyList(), "HU");
        assertEquals(CashValue.of(BigDecimal.ZERO, null), tracker.getFeeWriteOffAvailable());
        assertEquals(Collections.emptyList(), tracker.getLossOffsetAvailable());
        assertEquals(Collections.emptyMap(), tracker.getSecurityPositions());
        assertEquals(Collections.emptyMap(), tracker.getBrokerCashBalances());
    }

    @Test
    void addToTracker_moneyIn() {
        Transaction moneyIn = Transaction.builder(LocalDate.EPOCH, TransactionType.MONEY_IN)
                .add(BigDecimal.TEN, new InvestmentAssetRecord(InvestmentAssetType.CASH, 10L), account)
                .build();

        ActualPositionTracker tracker = mock(ActualPositionTracker.class);
        transactionProcessor.addTransactionsToTracker(tracker, List.of(moneyIn), "HU");
        verify(tracker).processMoneyIn(LocalDate.EPOCH, account, "EUR", BigDecimal.TEN);
    }

    @Test
    void addToTracker_moneyOut() {
        Transaction moneyIn = Transaction.builder(LocalDate.EPOCH, TransactionType.MONEY_OUT)
                .take(BigDecimal.TEN, new InvestmentAssetRecord(InvestmentAssetType.CASH, 10L), account)
                .build();

        ActualPositionTracker tracker = mock(ActualPositionTracker.class);
        transactionProcessor.addTransactionsToTracker(tracker, List.of(moneyIn), "HU");
        verify(tracker).processMoneyOut(LocalDate.EPOCH, account, "EUR", BigDecimal.TEN);
    }


    @Test
    void addToTracker_enterInvestment() {
        Transaction moneyIn = Transaction.builder(LocalDate.EPOCH, TransactionType.ENTER_INVESTMENT)
                .add(BigDecimal.ONE, new InvestmentAssetRecord(InvestmentAssetType.SECURITY, 20L), account)
                .take(BigDecimal.TEN, new InvestmentAssetRecord(InvestmentAssetType.CASH, 10L), account)
                .build();

        ActualPositionTracker tracker = mock(ActualPositionTracker.class);
        transactionProcessor.addTransactionsToTracker(tracker, List.of(moneyIn), "HU");
        verify(tracker).processEnterInvestment(LocalDate.EPOCH, account, security, BigDecimal.ONE, "EUR", BigDecimal.TEN);
    }


    @Test
    void addToTracker_exitInvestment() {
        Transaction moneyIn = Transaction.builder(LocalDate.EPOCH, TransactionType.EXIT_INVESTMENT)
                .add(BigDecimal.TEN, new InvestmentAssetRecord(InvestmentAssetType.CASH, 10L), account)
                .take(BigDecimal.ONE, new InvestmentAssetRecord(InvestmentAssetType.SECURITY, 20L), account)
                .matching(MatchingStrategy.FIFO)
                .build();

        ActualPositionTracker tracker = mock(ActualPositionTracker.class);

        TaxEffectDto tax = TaxEffectDto.builder()
                .taxPaid(CashValue.of("1", "HUF"))
                .feeUsed(CashValue.of("3", "HUF")).build();
        when(taxCalculator.getTaxEffect(eq(tracker), anyList(), eq(LocalDate.EPOCH), eq(account), eq(CashValue.of("10", "EUR")), eq("HU")))
                .thenReturn(tax);

        transactionProcessor.addTransactionsToTracker(tracker, List.of(moneyIn), "HU");
        verify(tracker).processExitInvestment(account, security, BigDecimal.ONE, CashValue.of("10", "EUR"), MatchingStrategy.FIFO);
        verify(tracker).processTax(tax);
    }

    @Test
    void addToTracker_transferSecurity() {
        Transaction moneyIn = Transaction.builder(LocalDate.EPOCH, TransactionType.TRANSFER_SECURITY)
                .take(BigDecimal.ONE, new InvestmentAssetRecord(InvestmentAssetType.SECURITY, 20L), account)
                .add(BigDecimal.ONE, new InvestmentAssetRecord(InvestmentAssetType.SECURITY, 20L), anotherAccount)
                .matching(MatchingStrategy.FIFO)
                .build();

        ActualPositionTracker tracker = mock(ActualPositionTracker.class);

        transactionProcessor.addTransactionsToTracker(tracker, List.of(moneyIn), "HU");
        verify(tracker).processTransferSecurity(account, anotherAccount, security, BigDecimal.ONE, MatchingStrategy.FIFO);
    }

    @Test
    void addToTracker_transferCash() {
        Transaction moneyIn = Transaction.builder(LocalDate.EPOCH, TransactionType.TRANSFER_CASH)
                .take(BigDecimal.ONE, new InvestmentAssetRecord(InvestmentAssetType.CASH, 10L), account)
                .add(BigDecimal.ONE, new InvestmentAssetRecord(InvestmentAssetType.CASH, 10L), anotherAccount)
                .matching(MatchingStrategy.FIFO)
                .build();

        ActualPositionTracker tracker = mock(ActualPositionTracker.class);

        transactionProcessor.addTransactionsToTracker(tracker, List.of(moneyIn), "HU");
        verify(tracker).processTransferCash(account, anotherAccount,"EUR", BigDecimal.ONE);
    }

    @Test
    void addToTracker_payFeeCurrencyCorrect() {
        Transaction moneyIn = Transaction.builder(LocalDate.EPOCH, TransactionType.PAY_FEE)
                .take(BigDecimal.ONE, new InvestmentAssetRecord(InvestmentAssetType.CASH, 10L), account)
                .matching(MatchingStrategy.FIFO)
                .build();

        when(taxCalculator.getCashInTaxCurrency(CashValue.of(BigDecimal.ONE, "EUR"), LocalDate.EPOCH, "HU"))
                .thenReturn(CashValue.of("10", "HUF"));

        ActualPositionTracker tracker = mock(ActualPositionTracker.class);
        transactionProcessor.addTransactionsToTracker(tracker, List.of(moneyIn), "HU");
        verify(tracker).processFee(account,"EUR", BigDecimal.ONE, CashValue.of("10", "HUF"));
    }
}