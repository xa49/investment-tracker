package app.preload;

import app.analysis.portfolio.CreatePortfolioCommand;
import app.analysis.portfolio.PortfolioRepository;
import app.analysis.portfolio.PortfolioService;
import app.broker.BrokerDto;
import app.broker.BrokerRepository;
import app.broker.BrokerService;
import app.broker.CreateBrokerCommand;
import app.broker.account.*;
import app.broker.account.association.CreateProductAssociationCommand;
import app.broker.account.association.ProductAssociationRepository;
import app.broker.fees.BrokerFeeService;
import app.broker.fees.transfer.BrokerTransferFeeRepository;
import app.broker.fees.transfer.CreateBrokerTransferFeeCommand;
import app.broker.product.BrokerProductDto;
import app.broker.product.BrokerProductRepository;
import app.broker.product.BrokerProductService;
import app.broker.product.CreateBrokerProductCommand;
import app.broker.product.commission.CreateCommissionCommand;
import app.broker.product.commission.ProductCommissionRepository;
import app.manager.transaction.*;
import app.taxation.details.CreateTaxDetailsCommand;
import app.taxation.details.TaxDetailsRepository;
import app.taxation.details.TaxDetailsService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class Preloader {

    private final BrokerService brokerService;
    private final BrokerFeeService feeService;
    private final BrokerProductService productService;
    private final BrokerAccountService accountService;
    private final TaxDetailsService taxDetailsService;
    private final PortfolioService portfolioService;
    private final TransactionService transactionService;

    private final BrokerRepository brokerRepository;
    private final BrokerTransferFeeRepository transferFeeRepository;
    private final BrokerProductRepository productRepository;
    private final ProductCommissionRepository productCommissionRepository;
    private final BrokerAccountRepository accountRepository;
    private final ProductAssociationRepository productAssociationRepository;
    private final TaxDetailsRepository taxDetailsRepository;
    private final PortfolioRepository portfolioRepository;
    private final TransactionRepository transactionRepository;

    public void reset() {
        transactionRepository.deleteAll();
        productAssociationRepository.deleteAll();
        transferFeeRepository.deleteAll();
        productCommissionRepository.deleteAll();
        productRepository.deleteAll();
        accountRepository.deleteAll();
        brokerRepository.deleteAll();
        taxDetailsRepository.deleteAll();
        portfolioRepository.deleteAll();

    }


    public void preload() {
        log.info("Preloading broker entities for testing the application...");

        BrokerDto broker = brokerService.addBroker(new CreateBrokerCommand("The Only Broker"));

        // Transferring EUR/USD out from broker costs 0.45%, min. 2,710 HUF, max. 66,000 HUF per transaction
        CreateBrokerTransferFeeCommand transferFeeCommand = CreateBrokerTransferFeeCommand.builder()
                .transferredCurrency("USD")
                .percentFee(new BigDecimal("0.45"))
                .minimumFee(new BigDecimal("2710"))
                .maximumFee(new BigDecimal("66000"))
                .feeCurrency("HUF")
                .build();
        feeService.addTransferFee(broker.getId(), transferFeeCommand);
        transferFeeCommand.setTransferredCurrency("EUR");
        feeService.addTransferFee(broker.getId(), transferFeeCommand);

        // There is a single type of trading account at the broker which can be used for both MAIN and TBSZ accounts
        BrokerProductDto product = productService.addProduct(broker.getId(), CreateBrokerProductCommand.builder()
                .name("regular trading account")
                .build());
        // It costs 0.35% and 7 USD/EUR to trade on NYSE and Amsterdam (XETRA)
        productService.addCommission(product.getId(), CreateCommissionCommand.builder()
                .market("NYSE")
                .percentFee(new BigDecimal("0.35"))
                .minimumFee(new BigDecimal("7"))
                .currency("USD")
                .build());
        productService.addCommission(product.getId(), CreateCommissionCommand.builder()
                .market("Amsterdam")
                .percentFee(new BigDecimal("0.35"))
                .minimumFee(new BigDecimal("7"))
                .currency("EUR")
                .build());

        // Adding personal main account
        BrokerAccountDto mainAccount = accountService.addAccount(CreateAccountCommand.builder()
                .accountType(BrokerAccountType.MAIN)
                .openedDate(LocalDate.of(2000, 1, 1))
                .name("personal main account")
                .build());
        // Adding a valid, ongoing TBSZ account
        BrokerAccountDto tbsz2000 = accountService.addAccount(CreateAccountCommand.builder()
                .accountType(BrokerAccountType.TBSZ)
                .openedDate(LocalDate.of(2000, 2, 2))
                .name("tbsz 2000 account")
                .build());
        // Adding a broker TBSZ account, which completed its first 5-year cycle but was later broker
        BrokerAccountDto brokenTbsz2001 = accountService.addAccount(CreateAccountCommand.builder()
                .accountType(BrokerAccountType.TBSZ)
                .openedDate(LocalDate.of(2001, 3, 3))
                .name("(eventually broken) tbsz 2001 account")
                .build());

        // Each account is associated with the only product at the broker
        accountService.addProductToAccount(mainAccount.getId(), CreateProductAssociationCommand.builder()
                .productId(product.getId())
                .fromDate(LocalDate.of(2000, 1, 1))
                .build());
        accountService.addProductToAccount(tbsz2000.getId(), CreateProductAssociationCommand.builder()
                .productId(product.getId())
                .fromDate(LocalDate.of(2000, 2, 2))
                .build());
        accountService.addProductToAccount(brokenTbsz2001.getId(), CreateProductAssociationCommand.builder()
                .productId(product.getId())
                .fromDate(LocalDate.of(2001, 3, 3))
                .build());

        portfolioService.addPortfolio(CreatePortfolioCommand.builder()
                .accountIds(List.of(mainAccount.getId(), tbsz2000.getId(), brokenTbsz2001.getId()))
                .name("my-portfolio")
                .build());


        log.info("Finished loading broker entities");

        log.info("Added tax detail for HU");
        taxDetailsService.addTaxDetails(CreateTaxDetailsCommand.builder()
                .taxResidence("HU")
                .flatCapitalGainsTaxRate(new BigDecimal("15"))
                .lossOffsetYears(2)
                .lossOffsetCutOffMonth(1)
                .lossOffsetCutOffDay(1)
                .taxationCurrency("HUF")
                .build());

        // Adding 10,000 USD to main account
        transactionService.addTransaction(CreateTransactionCommand.builder()
                .transactionType(TransactionType.MONEY_IN)
                .date(LocalDate.of(2000, 1, 2))
                .addToAccountId(mainAccount.getId())
                .assetAddedId("USD")
                .countOfAssetAdded(new BigDecimal("10000"))
                .build());

        // Buying 100 MMM shares on main account @ 40 USD each
        transactionService.addTransaction(CreateTransactionCommand.builder()
                .transactionType(TransactionType.ENTER_INVESTMENT)
                .date(LocalDate.of(2000, 1, 30))
                .assetAddedId("MMM")
                .countOfAssetAdded(new BigDecimal("100"))
                .addToAccountId(mainAccount.getId())
                .assetTakenId("USD")
                .countOfAssetTaken(new BigDecimal("4000"))
                .takeFromAccountId(mainAccount.getId())
                .build());

        // Transferring 2500 USD to TBSZ 2000 account
        transactionService.addTransaction(CreateTransactionCommand.builder()
                .transactionType(TransactionType.TRANSFER_CASH)
                .date(LocalDate.of(2000, 2, 10))
                .assetTakenId("USD")
                .countOfAssetTaken(new BigDecimal("2500"))
                .takeFromAccountId(mainAccount.getId())
                .assetAddedId("USD")
                .countOfAssetAdded(new BigDecimal("2500"))
                .addToAccountId(tbsz2000.getId())
                .build());

        // Buying 40 Johnson & Johnson shares on TBSZ 2000 account @ 52 USD each
        transactionService.addTransaction(CreateTransactionCommand.builder()
                .transactionType(TransactionType.ENTER_INVESTMENT)
                .date(LocalDate.of(2000, 3, 1))
                .assetAddedId("JNJ")
                .countOfAssetAdded(new BigDecimal("40"))
                .addToAccountId(tbsz2000.getId())
                .assetTakenId("USD")
                .countOfAssetTaken(new BigDecimal("2080"))
                .takeFromAccountId(tbsz2000.getId())
                .build());

        // Transferring 2500 USD to TBSZ 2001 account
        transactionService.addTransaction(CreateTransactionCommand.builder()
                .transactionType(TransactionType.TRANSFER_CASH)
                .date(LocalDate.of(2001, 2, 10))
                .assetTakenId("USD")
                .countOfAssetTaken(new BigDecimal("2500"))
                .takeFromAccountId(mainAccount.getId())
                .assetAddedId("USD")
                .countOfAssetAdded(new BigDecimal("2500"))
                .addToAccountId(brokenTbsz2001.getId())
                .build());

        // Buying 30 MMM shares on TBSZ 2001 account @ 55 USD each
        transactionService.addTransaction(CreateTransactionCommand.builder()
                .transactionType(TransactionType.ENTER_INVESTMENT)
                .date(LocalDate.of(2001, 3, 4))
                .assetAddedId("MMM")
                .countOfAssetAdded(new BigDecimal("30"))
                .addToAccountId(brokenTbsz2001.getId())
                .assetTakenId("USD")
                .countOfAssetTaken(new BigDecimal("1650"))
                .takeFromAccountId(brokenTbsz2001.getId())
                .build());

        // Breaking TBSZ 2001 account in 2008 by selling 30 MMM shares @ 68 USD each
        transactionService.addTransaction(CreateTransactionCommand.builder()
                .transactionType(TransactionType.EXIT_INVESTMENT)
                .date(LocalDate.of(2008, 3, 4))
                .assetTakenId("MMM")
                .countOfAssetTaken(new BigDecimal("20"))
                .takeFromAccountId(brokenTbsz2001.getId())
                .assetAddedId("USD")
                .countOfAssetAdded(new BigDecimal("1360"))
                .addToAccountId(brokenTbsz2001.getId())
                .matchingStrategy(MatchingStrategy.FIFO)
                .build());

    }
}
