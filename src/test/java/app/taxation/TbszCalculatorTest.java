package app.taxation;

import app.analysis.CashValue;
import app.broker.account.BrokerAccount;
import app.broker.account.BrokerAccountType;
import app.data.DataService;
import app.analysis.actual.SecurityPosition;
import app.data.securities.price.SecurityPriceOfDay;
import app.data.securities.security.Security;
import app.manager.transaction.Transaction;
import app.manager.transaction.TransactionService;
import app.manager.transaction.TransactionType;
import app.taxation.details.TaxDetails;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TbszCalculatorTest {

    @Mock
    TransactionService transactionService;

    @Mock
    DataService dataService;

    @InjectMocks
    TbszCalculator tbszCalculator;

    BrokerAccount startOfYear2000 = createBrokerAccount(BrokerAccountType.TBSZ, LocalDate.of(2000, 1, 1));
    BrokerAccount endOfYear2000 = createBrokerAccount(BrokerAccountType.TBSZ, LocalDate.of(2000, 12, 31));
    BrokerAccount startOfYear2001 = createBrokerAccount(BrokerAccountType.TBSZ, LocalDate.of(2001, 1, 1));
    BrokerAccount notATbszStartOfYear2000 = createBrokerAccount(BrokerAccountType.MAIN, LocalDate.of(2000, 1, 1));

    Security security = new Security("ticker", "name", "Amsterdam", "EUR");

    TaxDetails taxDetailsHU = new TaxDetails();

    @BeforeEach
    void init() {
        taxDetailsHU.setTaxResidence("HU");
        taxDetailsHU.setTaxationCurrency("HUF");
        taxDetailsHU.setFlatCapitalGainsTaxRate(new BigDecimal(15));
    }

    @Test
    void isEligible_noExitsOnAccount() {
        when(transactionService.getTransactionsOnTakeAccountByType(anyLong(), anyList()))
                .thenReturn(Collections.emptyList());
        LocalDate endOfYear2005 = LocalDate.of(2005, 12, 31); // TBSZ opened in 1999 or earlier are eligible. The ones opened in 2000 are in their fifth year.
        assertFalse(tbszCalculator.isEligible(startOfYear2000, endOfYear2005));
        assertFalse(tbszCalculator.isEligible(endOfYear2000, endOfYear2005));
        assertFalse(tbszCalculator.isEligible(startOfYear2001, endOfYear2005));
        assertFalse(tbszCalculator.isEligible(notATbszStartOfYear2000, endOfYear2005));

        LocalDate startOfYear2006 = LocalDate.of(2006, 1, 1); // TBSZ opened in 2000 becomes eligible.
        assertTrue(tbszCalculator.isEligible(startOfYear2000, startOfYear2006));
        assertTrue(tbszCalculator.isEligible(endOfYear2000, startOfYear2006));
        assertFalse(tbszCalculator.isEligible(startOfYear2001, startOfYear2006));
        assertFalse(tbszCalculator.isEligible(notATbszStartOfYear2000, startOfYear2006));
    }

    @Test
    void isEligible_exitsAfterFullCycle() {
        Transaction validExit = Transaction.builder(LocalDate.of(2006, 1, 1), TransactionType.TRANSFER_CASH).build();
        validExit.setAddToAccount(startOfYear2000);
        validExit.setTakeFromAccount(startOfYear2000);
        when(transactionService.getTransactionsOnTakeAccountByType(eq(startOfYear2000.getId()), anyList()))
                .thenReturn(List.of(validExit));
        LocalDate startOfYear2006 = LocalDate.of(2006, 1, 1);
        assertTrue(tbszCalculator.isEligible(startOfYear2000, startOfYear2006));
    }

    @Test
    void notEligible_noFullCycleTbsz() {
        Transaction invalidExit = Transaction.builder(LocalDate.of(2005, 12, 31), TransactionType.EXIT_INVESTMENT).build();
        invalidExit.setAddToAccount(startOfYear2000);
        invalidExit.setTakeFromAccount(startOfYear2000);
        when(transactionService.getTransactionsOnTakeAccountByType(eq(startOfYear2000.getId()), anyList()))
                .thenReturn(List.of(invalidExit));
        LocalDate startOfYear2006 = LocalDate.of(2006, 1, 1); // TBSZ opened in 2000 becomes eligible.
        assertFalse(tbszCalculator.isEligible(startOfYear2000, startOfYear2006));
    }

    @Test
    void calculateGains_validAccount_withValidExit() {
        Transaction validExit = Transaction.builder(LocalDate.of(2006, 1, 1), TransactionType.EXIT_INVESTMENT)
                .add(null, null, endOfYear2000)
                .take(null, null, endOfYear2000)
                .build();
        when(transactionService.getTransactionsOnTakeAccountByType(anyLong(), anyList()))
                .thenReturn(List.of(validExit));

        when(dataService.getSharePrice("ticker", LocalDate.of(2005, 12, 31)))
                .thenReturn(SecurityPriceOfDay.builder()
                        .ticker("ticker")
                        .date(LocalDate.of(2005, 12, 31))
                        .currency("EUR")
                        .close(new BigDecimal("3.1"))
                        .build());
        when(dataService.getExchangeRate("EUR", "HUF", LocalDate.of(2008,5,5)))
                .thenReturn(new BigDecimal("2"));
        when(dataService.getExchangeRate("EUR", "EUR", LocalDate.of(2008,5,5)))
                .thenReturn(BigDecimal.ONE);

        List<SecurityPosition> positions = List.of(
                new SecurityPosition(security, BigDecimal.TEN, CashValue.of("3", "EUR"), LocalDate.of(2000, 5, 4)),
                new SecurityPosition(security, BigDecimal.TEN, CashValue.of("2", "EUR"), LocalDate.of(2000, 7, 4)),
                new SecurityPosition(security, BigDecimal.TEN, CashValue.of("1", "EUR"), LocalDate.of(2000, 9, 1))
        );

        // TBSZ broken after first 5-year cycle. Security price was 3.1 at the end of 5-year cycle, i.e. eligible costs are 3 x 10 x 3.1 = 93.0
        // Total gain = 100 proceeds - 93 gains = 7 EUR
        CashValue gains = tbszCalculator.calculateGains(endOfYear2000, LocalDate.of(2008, 5, 5),
                CashValue.of("100", "EUR"), positions, taxDetailsHU);
        assertEquals(CashValue.of("14.0", "HUF"), gains);
    }

    @Test
    void calculateGains_validAccount_withValidExit_sharePriceLowAtTimeOfExit() {
        Transaction validExit = Transaction.builder(LocalDate.of(2006, 1, 1), TransactionType.EXIT_INVESTMENT)
                .add(null, null, endOfYear2000)
                .take(null, null, endOfYear2000)
                .build();
        when(transactionService.getTransactionsOnTakeAccountByType(anyLong(), anyList()))
                .thenReturn(List.of(validExit));

        when(dataService.getSharePrice("ticker", LocalDate.of(2005, 12, 31)))
                .thenReturn(SecurityPriceOfDay.builder()
                        .ticker("ticker")
                        .date(LocalDate.of(2005, 12, 31))
                        .currency("EUR")
                        .close(new BigDecimal("1.5"))
                        .build());
        when(dataService.getExchangeRate("EUR", "HUF", LocalDate.of(2008,5,5)))
                .thenReturn(new BigDecimal("2"));
        when(dataService.getExchangeRate("EUR", "EUR", LocalDate.of(2008,5,5)))
                .thenReturn(BigDecimal.ONE);

        List<SecurityPosition> positions = List.of(
                new SecurityPosition(security, BigDecimal.TEN, CashValue.of("3", "EUR"), LocalDate.of(2000, 5, 4)),
                new SecurityPosition(security, BigDecimal.TEN, CashValue.of("2", "EUR"), LocalDate.of(2000, 7, 4)),
                new SecurityPosition(security, BigDecimal.TEN, CashValue.of("1", "EUR"), LocalDate.of(2000, 9, 1))
        );

        // TBSZ broken after first 5-year cycle. Security price was 1.5 at the end of 5-year cycle
        // Eligible cost is max(1.5, 3) x 10 + max(1.5, 2) x 10 + max(1.5, 1) x 10 = 65. Gain is 35 EUR x 2 = 70 HUF
        CashValue gains = tbszCalculator.calculateGains(endOfYear2000, LocalDate.of(2008, 5, 5),
                CashValue.of("100", "EUR"), positions, taxDetailsHU);
        assertEquals(CashValue.of("70.0", "HUF"), gains);
    }

    @Test
    void calculateGains_validAccount_withValidExit_differentCurrencyProceeds() {
        Transaction validExit = Transaction.builder(LocalDate.of(2006, 1, 1), TransactionType.EXIT_INVESTMENT)
                .add(null, null, endOfYear2000)
                .take(null, null, endOfYear2000)
                .build();
        when(transactionService.getTransactionsOnTakeAccountByType(anyLong(), anyList()))
                .thenReturn(List.of(validExit));

        when(dataService.getSharePrice("ticker", LocalDate.of(2005, 12, 31)))
                .thenReturn(SecurityPriceOfDay.builder()
                        .ticker("ticker")
                        .date(LocalDate.of(2005, 12, 31))
                        .currency("EUR")
                        .close(new BigDecimal("1.5"))
                        .build());
        when(dataService.getExchangeRate("USD", "HUF", LocalDate.of(2008,5,5)))
                .thenReturn(new BigDecimal("5"));
        when(dataService.getExchangeRate("EUR", "HUF", LocalDate.of(2008,5,5)))
                .thenReturn(new BigDecimal("2"));
        when(dataService.getExchangeRate("EUR", "EUR", LocalDate.of(2008,5,5)))
                .thenReturn(BigDecimal.ONE);

        List<SecurityPosition> positions = List.of(
                new SecurityPosition(security, BigDecimal.TEN, CashValue.of("3", "EUR"), LocalDate.of(2000, 5, 4)),
                new SecurityPosition(security, BigDecimal.TEN, CashValue.of("2", "EUR"), LocalDate.of(2000, 7, 4)),
                new SecurityPosition(security, BigDecimal.TEN, CashValue.of("1", "EUR"), LocalDate.of(2000, 9, 1))
        );

        // TBSZ broken after first 5-year cycle. Security price was 1.5 at the end of 5-year cycle
        // Eligible cost is max(1.5, 3) x 10 + max(1.5, 2) x 10 + max(1.5, 1) x 10 = 65 EUR
        // Proceeds = 5 x 100 = 500 HUF less costs 2 x 65 = 370 HUF
        CashValue gains = tbszCalculator.calculateGains(endOfYear2000, LocalDate.of(2008, 5, 5),
                CashValue.of("100", "USD"), positions, taxDetailsHU);
        assertEquals(CashValue.of("370.0", "HUF"), gains);
    }

    @Test
    void calculateGains_correctTransactionTypesAsBreakingTransactions() {
        Transaction validExit = Transaction.builder(LocalDate.of(2006, 1, 1), TransactionType.EXIT_INVESTMENT)
                .add(null, null, endOfYear2000)
                .take(null, null, endOfYear2000)
                .build();
        when(transactionService.getTransactionsOnTakeAccountByType(anyLong(), anyList()))
                .thenReturn(List.of(validExit));

        when(dataService.getSharePrice("ticker", LocalDate.of(2005, 12, 31)))
                .thenReturn(SecurityPriceOfDay.builder()
                        .ticker("ticker")
                        .date(LocalDate.of(2005, 12, 31))
                        .currency("EUR")
                        .close(new BigDecimal("1.5"))
                        .build());
        when(dataService.getExchangeRate("USD", "HUF", LocalDate.of(2008,5,5)))
                .thenReturn(new BigDecimal("5"));
        when(dataService.getExchangeRate("EUR", "HUF", LocalDate.of(2008,5,5)))
                .thenReturn(new BigDecimal("2"));
        when(dataService.getExchangeRate("EUR", "EUR", LocalDate.of(2008,5,5)))
                .thenReturn(BigDecimal.ONE);

        List<SecurityPosition> positions = List.of(
                new SecurityPosition(security, BigDecimal.TEN, CashValue.of("1", "EUR"), LocalDate.of(2000, 9, 1))
        );


        tbszCalculator.calculateGains(endOfYear2000, LocalDate.of(2008, 5, 5),
                CashValue.of("100", "USD"), positions, taxDetailsHU);
        verify(transactionService).getTransactionsOnTakeAccountByType(eq(endOfYear2000.getId()),
                argThat(l -> l.equals(List.of(TransactionType.EXIT_INVESTMENT, TransactionType.TRANSFER_CASH, TransactionType.TRANSFER_SECURITY))));

    }

    @Test
    void validAccount_TwoPeriods_ValidExit() {
        Transaction validExit = Transaction.builder(LocalDate.of(2006, 1, 1), TransactionType.EXIT_INVESTMENT).build();
        validExit.setAddToAccount(endOfYear2000);
        validExit.setTakeFromAccount(endOfYear2000);
        when(transactionService.getTransactionsOnTakeAccountByType(anyLong(), anyList()))
                .thenReturn(List.of(validExit));

        when(dataService.getSharePrice("ticker", LocalDate.of(2010, 12, 31)))
                .thenReturn(SecurityPriceOfDay.builder()
                        .ticker("ticker")
                        .date(LocalDate.of(2005, 12, 31))
                        .currency("EUR")
                        .close(new BigDecimal("3.1"))
                        .build());
        when(dataService.getExchangeRate("EUR", "HUF", LocalDate.of(2015,5,5)))
                .thenReturn(new BigDecimal("2"));
        when(dataService.getExchangeRate("EUR", "EUR", LocalDate.of(2015,5,5)))
                .thenReturn(BigDecimal.ONE);

        List<SecurityPosition> positions = List.of(
                new SecurityPosition(security, BigDecimal.TEN, new CashValue(new BigDecimal("3"), "EUR"), LocalDate.of(2000, 5, 4)),
                new SecurityPosition(security, BigDecimal.TEN, new CashValue(new BigDecimal("2"), "EUR"), LocalDate.of(2000, 7, 4)),
                new SecurityPosition(security, BigDecimal.TEN, new CashValue(new BigDecimal("1"), "EUR"), LocalDate.of(2000, 9, 1))
        );


        CashValue gains = tbszCalculator.calculateGains(endOfYear2000, LocalDate.of(2015, 5, 5),
                new CashValue(new BigDecimal("100"), "EUR"), positions, taxDetailsHU);
        assertEquals(new CashValue(new BigDecimal("14.0"), "HUF"), gains);
    }

    @Test
    void validFirstCycle_BrokenIn2007() {
        Transaction breakingTransaction = Transaction.builder(LocalDate.of(2007, 1, 1), TransactionType.TRANSFER_SECURITY).build();
        breakingTransaction.setAddToAccount(endOfYear2000);
        breakingTransaction.setTakeFromAccount(endOfYear2000);
        when(transactionService.getTransactionsOnTakeAccountByType(anyLong(), anyList()))
                .thenReturn(List.of(breakingTransaction));

        when(dataService.getSharePrice("ticker", LocalDate.of(2007, 1, 1)))
                .thenReturn(SecurityPriceOfDay.builder()
                        .ticker("ticker")
                        .date(LocalDate.of(2005, 12, 31))
                        .currency("EUR")
                        .close(new BigDecimal("3.1"))
                        .build());
        when(dataService.getExchangeRate("EUR", "HUF", LocalDate.of(2015,5,5)))
                .thenReturn(new BigDecimal("2"));
        when(dataService.getExchangeRate("EUR", "EUR", LocalDate.of(2015,5,5)))
                .thenReturn(BigDecimal.ONE);

        List<SecurityPosition> positions = List.of(
                new SecurityPosition(security, BigDecimal.TEN, new CashValue(new BigDecimal("3"), "EUR"), LocalDate.of(2000, 5, 4)),
                new SecurityPosition(security, BigDecimal.TEN, new CashValue(new BigDecimal("2"), "EUR"), LocalDate.of(2000, 7, 4)),
                new SecurityPosition(security, BigDecimal.TEN, new CashValue(new BigDecimal("1"), "EUR"), LocalDate.of(2000, 9, 1))
        );


        CashValue gains = tbszCalculator.calculateGains(endOfYear2000, LocalDate.of(2015, 5, 5),
                new CashValue(new BigDecimal("100"), "EUR"), positions, taxDetailsHU);
        assertEquals(new CashValue(new BigDecimal("14.0"), "HUF"), gains);
    }

    private BrokerAccount createBrokerAccount(BrokerAccountType type, LocalDate opened) {
        BrokerAccount account = new BrokerAccount();
        account.setAccountType(type);
        account.setOpenedDate(opened);
        account.setId(123L);
        return account;
    }

}