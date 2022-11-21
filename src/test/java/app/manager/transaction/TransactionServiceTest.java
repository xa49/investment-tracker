package app.manager.transaction;

import app.analysis.CashValue;
import app.broker.BrokerEntityNotFoundException;
import app.broker.account.BrokerAccountService;
import app.broker.fees.calculator.FeeCalculatorService;
import app.data.DataService;
import app.manager.transaction.asset_record.InvestmentAssetRecord;
import app.manager.transaction.asset_record.InvestmentAssetRecordService;
import app.manager.transaction.asset_record.InvestmentAssetType;
import app.taxation.TbszValidator;
import app.util.InvalidDataException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    BrokerAccountService accountService;
    @Mock
    InvestmentAssetRecordService recordService;
    @Mock
    TransactionRepository transactionRepository;
    @Mock
    FeeCalculatorService feeCalculatorService;
    @Mock
    DataService dataService;
    @Mock
    TbszValidator tbszValidator;
    @Mock
    EntityManager entityManager;
    @Spy
    TransactionMapperImpl mapper;

    @InjectMocks
    TransactionService transactionService;

    InvestmentAssetRecord eurRecord = new InvestmentAssetRecord(InvestmentAssetType.CASH, 10L);
    InvestmentAssetRecord hufRecord = new InvestmentAssetRecord(InvestmentAssetType.CASH, 11L);
    InvestmentAssetRecord mmmRecord = new InvestmentAssetRecord(InvestmentAssetType.SECURITY, 12L);

    @BeforeEach
    void init() {
        eurRecord.setId(8L);
        hufRecord.setId(9L);
        mmmRecord.setId(10L);

        // Initially setup
        // Account id 4 is present
        lenient().when(accountService.accountCountById(4L))
                .thenReturn(1);
        // EUR cash record is present
        lenient().when(recordService.getCashRecord("EUR"))
                .thenReturn(eurRecord);
        // HUF cash record is present
        lenient().when(recordService.getCashRecord("HUF"))
                .thenReturn(hufRecord);
        // MMM security is present
        lenient().when(recordService.getSecurityRecord("MMM"))
                .thenReturn(mmmRecord);
        // Market for MMM is NYSE
        lenient().when(dataService.getSecurityMarketByTicker("MMM"))
                .thenReturn("NYSE");
    }

    @Test
    void addingMoneyIn() {
        CreateTransactionCommand command = CreateTransactionCommand.builder()
                .date(LocalDate.of(2000, 1, 1))
                .transactionType(TransactionType.MONEY_IN)
                .addToAccountId(4L)
                .assetAddedId("EUR")
                .countOfAssetAdded(BigDecimal.TEN)
                .build();

        List<TransactionDto> recordedTransactions = transactionService.addTransaction(command);
        assertEquals(1, recordedTransactions.size());
        assertEquals(8L, recordedTransactions.get(0).getAssetAdded().getId());
        verify(transactionRepository).saveAll(argThat((List<Transaction> t) -> t.size() == 1 && t.get(0).getTransactionType() == TransactionType.MONEY_IN));
    }

    @Test
    void addingMoneyInCurrencyNotFound() {
        // Overriding: EUR asset record is invalid
        when(recordService.getCashRecord("EUR"))
                .thenThrow(new InvalidDataException("No such currency"));

        CreateTransactionCommand command = CreateTransactionCommand.builder()
                .date(LocalDate.of(2000, 1, 1))
                .transactionType(TransactionType.MONEY_IN)
                .addToAccountId(4L)
                .assetAddedId("EUR")
                .countOfAssetAdded(BigDecimal.TEN)
                .build();

        assertThrows(InvalidDataException.class, () -> transactionService.addTransaction(command));
    }

    @Test
    void addingMoneyInAccountNotFound() {
        // Overriding: account id 4 is not present
        when(accountService.accountCountById(4L))
                .thenReturn(0);

        CreateTransactionCommand command = CreateTransactionCommand.builder()
                .date(LocalDate.of(2000, 1, 1))
                .transactionType(TransactionType.MONEY_IN)
                .addToAccountId(4L)
                .assetAddedId("EUR")
                .countOfAssetAdded(BigDecimal.TEN)
                .build();

        InvalidDataException ex = assertThrows(InvalidDataException.class,
                () -> transactionService.addTransaction(command));
        assertEquals("No broker account found with database id: 4", ex.getMessage());
    }

    @Test
    void addingMoneyOut() {
        when(feeCalculatorService.getTransferFee(4L, LocalDate.of(2000, 1, 1), BigDecimal.TEN, "EUR"))
                .thenReturn(new CashValue(BigDecimal.ONE.negate(), "HUF"));

        CreateTransactionCommand command = CreateTransactionCommand.builder()
                .date(LocalDate.of(2000, 1, 1))
                .transactionType(TransactionType.MONEY_OUT)
                .takeFromAccountId(4L)
                .assetTakenId("EUR")
                .countOfAssetTaken(BigDecimal.TEN)
                .build();

        List<TransactionDto> recordedTransactions = transactionService.addTransaction(command);
        assertEquals(2, recordedTransactions.size());

        TransactionDto outTransaction = recordedTransactions.stream().filter(t -> t.getTransactionType() == TransactionType.MONEY_OUT).findFirst().orElseThrow();
        TransactionDto feeTransaction = recordedTransactions.stream().filter(t -> t.getTransactionType() == TransactionType.PAY_FEE).findFirst().orElseThrow();
        assertEquals(8L, outTransaction.getAssetTaken().getId());
        assertEquals(10L, outTransaction.getAssetTaken().getAssetId());

        assertEquals(BigDecimal.ONE, feeTransaction.getCountOfAssetTaken());
        assertEquals(11L, feeTransaction.getAssetTaken().getAssetId());
    }

    @Test
    void addingMoneyOutBrokerNotFound() {
        when(feeCalculatorService.getTransferFee(4L, LocalDate.of(2000, 1, 1), BigDecimal.TEN, "EUR"))
                .thenThrow(new BrokerEntityNotFoundException("Broker not found"));

        CreateTransactionCommand command = CreateTransactionCommand.builder()
                .date(LocalDate.of(2000, 1, 1))
                .transactionType(TransactionType.MONEY_OUT)
                .takeFromAccountId(4L)
                .assetTakenId("EUR")
                .countOfAssetTaken(BigDecimal.TEN)
                .build();

        assertThrows(BrokerEntityNotFoundException.class, () -> transactionService.addTransaction(command));
    }

    @Test
    void addingMoneyOutTransferFeeNotFound() {
        when(feeCalculatorService.getTransferFee(4L, LocalDate.of(2000, 1, 1), BigDecimal.TEN, "EUR"))
                .thenThrow(new BrokerEntityNotFoundException("Transfer fee not found"));

        CreateTransactionCommand command = CreateTransactionCommand.builder()
                .date(LocalDate.of(2000, 1, 1))
                .transactionType(TransactionType.MONEY_OUT)
                .takeFromAccountId(4L)
                .assetTakenId("EUR")
                .countOfAssetTaken(BigDecimal.TEN)
                .build();

        assertThrows(BrokerEntityNotFoundException.class, () -> transactionService.addTransaction(command));
    }

    @Test
    void addingEnterInvestment() {
        when(feeCalculatorService.getCommissionOnTransaction(4L, LocalDate.of(2000, 1, 1), "NYSE", BigDecimal.TEN, "EUR"))
                .thenReturn(new CashValue(BigDecimal.ONE.negate(), "EUR"));

        CreateTransactionCommand command = CreateTransactionCommand.builder()
                .date(LocalDate.of(2000, 1, 1))
                .transactionType(TransactionType.ENTER_INVESTMENT)
                .takeFromAccountId(4L)
                .assetTakenId("EUR")
                .countOfAssetTaken(BigDecimal.TEN)
                .addToAccountId(4L)
                .assetAddedId("MMM")
                .countOfAssetAdded(BigDecimal.ONE)
                .build();

        List<TransactionDto> recordedTransactions = transactionService.addTransaction(command);
        assertEquals(2, recordedTransactions.size());

        TransactionDto investmentTransaction = recordedTransactions.stream().filter(t -> t.getTransactionType() == TransactionType.ENTER_INVESTMENT).findFirst().orElseThrow();
        TransactionDto feeTransaction = recordedTransactions.stream().filter(t -> t.getTransactionType() == TransactionType.PAY_FEE).findFirst().orElseThrow();

        assertEquals(12L, investmentTransaction.getAssetAdded().getAssetId());
        assertEquals(10L, investmentTransaction.getAssetTaken().getAssetId());
        assertEquals(BigDecimal.ONE, feeTransaction.getFeeAmount());
    }

    @Test
    void addingEnterInvestmentToTbszAccountOutsideCollectionYear() {
        doThrow(new InvalidDataException("Adding to TBSZ outside collection year"))
                .when(tbszValidator).validateForTbszEntryConstraint(LocalDate.of(2000, 1, 1), 4L);

        CreateTransactionCommand command = CreateTransactionCommand.builder()
                .date(LocalDate.of(2000, 1, 1))
                .transactionType(TransactionType.ENTER_INVESTMENT)
                .takeFromAccountId(4L)
                .assetTakenId("EUR")
                .countOfAssetTaken(BigDecimal.TEN)
                .addToAccountId(4L)
                .assetAddedId("MMM")
                .countOfAssetAdded(BigDecimal.ONE)
                .build();

        assertThrows(InvalidDataException.class, () -> transactionService.addTransaction(command));
    }

    @Test
    void addingEnterInvestmentCommissionMissing() {
        when(feeCalculatorService.getCommissionOnTransaction(4L, LocalDate.of(2000, 1, 1), "NYSE", BigDecimal.TEN, "EUR"))
                .thenThrow(new BrokerEntityNotFoundException("Missing data for fee"));

        CreateTransactionCommand command = CreateTransactionCommand.builder()
                .date(LocalDate.of(2000, 1, 1))
                .transactionType(TransactionType.ENTER_INVESTMENT)
                .takeFromAccountId(4L)
                .assetTakenId("EUR")
                .countOfAssetTaken(BigDecimal.TEN)
                .addToAccountId(4L)
                .assetAddedId("MMM")
                .countOfAssetAdded(BigDecimal.ONE)
                .build();

        assertThrows(BrokerEntityNotFoundException.class, () -> transactionService.addTransaction(command));
    }

    @Test
    void updateEnterInvestment() {
        Transaction blankTransaction = new Transaction();
        blankTransaction.setId(111L);
        when(transactionRepository.findById(111L))
                .thenReturn(Optional.of(blankTransaction));

        UpdateTransactionCommand command = UpdateTransactionCommand.builder()
                .date(LocalDate.of(2000, 1, 1))
                .transactionType(TransactionType.ENTER_INVESTMENT)
                .takeFromAccountId(4L)
                .assetTakenId("EUR")
                .countOfAssetTaken(BigDecimal.TEN)
                .addToAccountId(4L)
                .assetAddedId("MMM")
                .countOfAssetAdded(BigDecimal.ONE)
                .build();

        transactionService.updateTransaction(111L, command);
        verify(entityManager).merge(argThat((Transaction t) ->
                t.getId() == 111L
                        && t.getTransactionType() == TransactionType.ENTER_INVESTMENT
                        && Objects.equals(t.getAssetTaken().getId(), eurRecord.getId())));
    }

    @Test
    void updateTransactionWithNoSuchId() {
        when(transactionRepository.findById(111L))
                .thenReturn(Optional.empty());

        UpdateTransactionCommand command = UpdateTransactionCommand.builder()
                .date(LocalDate.of(2000, 1, 1))
                .transactionType(TransactionType.ENTER_INVESTMENT)
                .takeFromAccountId(4L)
                .assetTakenId("EUR")
                .countOfAssetTaken(BigDecimal.TEN)
                .addToAccountId(4L)
                .assetAddedId("MMM")
                .countOfAssetAdded(BigDecimal.ONE)
                .build();

       InvalidDataException ex = assertThrows(InvalidDataException.class,
               () -> transactionService.updateTransaction(111L, command)) ;
       assertEquals("No transaction found with database id: 111", ex.getMessage());

       verify(feeCalculatorService, never()).getCommissionOnTransaction(anyLong(), any(), any(), any(), any());
    }
}