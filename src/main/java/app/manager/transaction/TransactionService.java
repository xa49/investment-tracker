package app.manager.transaction;

import app.analysis.CashValue;
import app.broker.account.BrokerAccount;
import app.broker.account.BrokerAccountService;
import app.broker.fees.FeeType;
import app.broker.fees.calculator.FeeCalculatorService;
import app.data.DataService;
import app.manager.transaction.asset_record.InvestmentAssetRecord;
import app.manager.transaction.asset_record.InvestmentAssetRecordService;
import app.taxation.TbszValidator;
import app.util.InvalidDataException;
import lombok.AllArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@AllArgsConstructor
public class TransactionService {

    private final BrokerAccountService accountService;
    private final InvestmentAssetRecordService recordService;
    private final TransactionRepository transactionRepository;
    private final FeeCalculatorService feeCalculatorService;
    private final DataService dataService;
    private final TbszValidator tbszValidator;
    private final TransactionMapper mapper;
    private final EntityManager entityManager;

    // Queries
    public List<Transaction> getTransactions(List<Long> accountIds, LocalDate from, LocalDate to) {
        return transactionRepository.getTransactionsOnAccountsInPeriod(accountIds, from, to);
    }

    public List<Transaction> getTransactionsUntil(List<Long> accountIds, LocalDate to) {
        return getTransactions(accountIds, null, to);
    }

    public List<Transaction> getTransactionsOnTakeAccountByType(
            Long accountId, List<TransactionType> breakingTransactions) {
        return transactionRepository.getTransactionsByTypeAndTakeAccount(accountId, breakingTransactions);
    }


    // Transactions
    public List<TransactionDto> addTransaction(CreateTransactionCommand command) {
        List<Transaction> transactions = getTransactions(command, ProcessType.ADD_FEES);
        transactionRepository.saveAll(transactions);
        return mapper.toDto(transactions);
    }

    @Transactional
    public void updateTransaction(Long transactionId, UpdateTransactionCommand command) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new InvalidDataException("No transaction found with database id: " + transactionId));

        Transaction modifiedTransaction = getTransactions(command, ProcessType.STANDALONE_TRANSACTION).stream()
                .filter(t -> t.getTransactionType() == command.getTransactionType())
                .findFirst().orElseThrow(() -> new IllegalStateException("No transaction matching type returned."));
        modifiedTransaction.setId(transaction.getId());
        entityManager.merge(modifiedTransaction);
    }

    public void deleteTransaction(Long transactionId) {
        try {
            transactionRepository.deleteById(transactionId);
        } catch (EmptyResultDataAccessException e) {
            throw new InvalidDataException("No transaction found with database id: " + transactionId);
        }
    }

    private List<Transaction> getTransactions(TransactionCommand command, ProcessType processType) {
        List<Transaction> transactions = new ArrayList<>();
        switch (command.getTransactionType()) {
            case MONEY_IN -> transactions = addMoneyToAccount(command.getAddToAccountId(), command.getDate(),
                    command.getCountOfAssetAdded(), command.getAssetAddedId());
            case MONEY_OUT -> transactions = withdrawMoneyFromAccount(command.getTakeFromAccountId(), command.getDate(),
                    command.getCountOfAssetTaken(), command.getAssetTakenId(), processType);
            case ENTER_INVESTMENT ->
                    transactions = enterInvestment(command.getAddToAccountId(), command.getAssetAddedId(),
                            command.getDate(), command.getCountOfAssetAdded(), command.getCountOfAssetTaken(),
                            command.getAssetTakenId(), processType);
            case EXIT_INVESTMENT -> transactions = exitInvestment(command, processType);
            case TRANSFER_SECURITY -> transactions = transferSecurityBetweenAccounts(command.getTakeFromAccountId(),
                    command.getAddToAccountId(), command.getDate(), command.getAssetAddedId(),
                    command.getCountOfAssetAdded(), command.getMatchingStrategy());
            case TRANSFER_CASH -> transactions = transferCashBetweenAccounts(command.getTakeFromAccountId(),
                    command.getAddToAccountId(), command.getDate(), command.getAssetAddedId(),
                    command.getCountOfAssetAdded());
            case PAY_FEE ->
                    transactions = payFee(command.getTakeFromAccountId(), command.getDate(), command.getAssetTakenId(),
                            command.getCountOfAssetTaken(), command.getFeeType());
        }
        return transactions;
    }

    private List<Transaction> addMoneyToAccount(Long accountId, LocalDate date, BigDecimal amount, String currency) {
        BrokerAccount account = getAccountReference(accountId);
        InvestmentAssetRecord asset = recordService.getCashRecord(currency);

        Transaction transaction = Transaction.builder(date, TransactionType.MONEY_IN)
                .add(amount, asset, account)
                .build();

        return List.of(transaction);
    }

    private List<Transaction> withdrawMoneyFromAccount(
            Long accountId, LocalDate date, BigDecimal amount, String currency, ProcessType processType) {
        BrokerAccount account = getAccountReference(accountId);
        InvestmentAssetRecord asset = recordService.getCashRecord(currency);

        List<Transaction> feeTransactions = new ArrayList<>();
        if (processType == ProcessType.ADD_FEES) {
            CashValue fee = feeCalculatorService.getTransferFee(accountId, date, amount, currency);
            feeTransactions = payFee(accountId, date, fee.getCurrency(), fee.getAmount().negate(), FeeType.TRANSFER);
        }

        Transaction transaction = Transaction.builder(date, TransactionType.MONEY_OUT)
                .take(amount, asset, account)
                .build();

        List<Transaction> transactions = new ArrayList<>(List.of(transaction));
        transactions.addAll(feeTransactions);
        return transactions;
    }

    private List<Transaction> enterInvestment(
            Long accountId, String ticker, LocalDate date, BigDecimal securityCount, BigDecimal totalPrice,
            String priceCurrency, ProcessType processType) {
        BrokerAccount account = getAccountReference(accountId);
        tbszValidator.validateForTbszEntryConstraint(date, accountId); // could change to account.getType().validateTransaction()
        InvestmentAssetRecord assetAdded = recordService.getSecurityRecord(ticker);

        InvestmentAssetRecord assetTaken = recordService.getCashRecord(priceCurrency);

        List<Transaction> feeTransactions = Collections.emptyList();
        if (processType == ProcessType.ADD_FEES) {
            String market = dataService.getSecurityMarketByTicker(ticker);
            CashValue fee = feeCalculatorService.getCommissionOnTransaction(accountId, date, market, totalPrice, priceCurrency);
            feeTransactions = payFee(accountId, date, fee.getCurrency(), fee.getAmount().negate(), FeeType.TRANSACTION_COMMISSION);
        }

        Transaction transaction = Transaction.builder(date, TransactionType.ENTER_INVESTMENT)
                .add(securityCount, assetAdded, account)
                .take(totalPrice, assetTaken, account)
                .build();


        List<Transaction> transactions = new ArrayList<>(List.of(transaction));
        transactions.addAll(feeTransactions);
        return transactions;
    }


    private List<Transaction> exitInvestment(TransactionCommand command, ProcessType processType) {
        BrokerAccount account = getAccountReference(command.getTakeFromAccountId());
        InvestmentAssetRecord assetAdded = recordService.getCashRecord(command.getAssetAddedId());

        InvestmentAssetRecord assetTaken = recordService.getSecurityRecord(command.getAssetTakenId());

        List<Transaction> feeTransactions = Collections.emptyList();
        if (processType == ProcessType.ADD_FEES) {
            String market = dataService.getSecurityMarketByTicker(command.getAssetTakenId());
            CashValue fee = feeCalculatorService
                    .getCommissionOnTransaction(account.getId(), command.getDate(), market,
                            command.getCountOfAssetAdded(), command.getAssetAddedId());
            feeTransactions = payFee(account.getId(), command.getDate(), fee.getCurrency(),
                    fee.getAmount().negate(), FeeType.TRANSACTION_COMMISSION);
        }

        Transaction transaction = Transaction.builder(command.getDate(), TransactionType.EXIT_INVESTMENT)
                .add(command.getCountOfAssetAdded(), assetAdded, account)
                .take(command.getCountOfAssetTaken(), assetTaken, account)
                .matching(command.getMatchingStrategy())
                .build();

        List<Transaction> transactions = new ArrayList<>(List.of(transaction));
        transactions.addAll(feeTransactions);
        return transactions;
    }

    private List<Transaction> transferSecurityBetweenAccounts(
            Long fromAccountId, Long toAccountId, LocalDate date,String ticker, BigDecimal count,
            MatchingStrategy matchingStrategy) {
        BrokerAccount fromAccount = getAccountReference(fromAccountId);
        BrokerAccount toAccount = getAccountReference(toAccountId);
        InvestmentAssetRecord security = recordService.getSecurityRecord(ticker);

        Transaction transaction = Transaction.builder(date, TransactionType.TRANSFER_SECURITY)
                .add(count, security, toAccount)
                .take(count, security, fromAccount)
                .matching(matchingStrategy)
                .build();

        return List.of(transaction);
    }

    private List<Transaction> transferCashBetweenAccounts(
            Long fromAccountId, Long toAccountId, LocalDate date, String currency, BigDecimal amount) {
        BrokerAccount fromAccount = getAccountReference(fromAccountId);
        BrokerAccount toAccount = getAccountReference(toAccountId);
        InvestmentAssetRecord asset = recordService.getCashRecord(currency);

        Transaction transaction = Transaction.builder(date, TransactionType.TRANSFER_CASH)
                .add(amount, asset, toAccount)
                .take(amount, asset, fromAccount)
                .build();

        return List.of(transaction);
    }

    private List<Transaction> payFee(
            Long accountId, LocalDate date, String currency, BigDecimal amount, FeeType feeType) {
        BrokerAccount account = getAccountReference(accountId);
        InvestmentAssetRecord asset = recordService.getCashRecord(currency);

        Transaction transaction = Transaction.builder(date, TransactionType.PAY_FEE)
                .take(amount, asset, account)
                .fee(amount, currency, feeType)
                .build();

        return List.of(transaction);
    }

    private BrokerAccount getAccountReference(Long accountId) {
        if (accountService.accountCountById(accountId) == 1) {
            return accountService.getReferenceById(accountId);
        }
        throw new InvalidDataException("No broker account found with database id: " + accountId);
    }
}
