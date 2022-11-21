package app.broker.account;

import app.broker.Broker;
import app.broker.BrokerEntityNotFoundException;
import app.broker.BrokerMapperImpl;
import app.broker.UniqueViolationException;
import app.broker.account.association.*;
import app.broker.product.BrokerProduct;
import app.util.DateRange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BrokerAccountServiceTest {

    @Mock
    BrokerAccountRepository accountRepository;

    @Spy
    BrokerMapperImpl mapper;

    @Spy
    BrokerAccountMapperImpl accountMapper;

    @Spy
    ProductAssociationMapperImpl associationMapper;

    @Mock
    ProductAssociationRepository associationRepository;

    @Mock
    ProductAssociationAdder associationAdder;

    @InjectMocks
    BrokerAccountService brokerAccountService;

    @Test
    void listAccounts() {
        BrokerAccount account1 = new BrokerAccount();
        BrokerAccount account2 = new BrokerAccount();
        account1.setName("one");
        account2.setName("two");
        when(accountRepository.findAll())
                .thenReturn(List.of(account1, account2));

        List<BrokerAccountDto> accounts = brokerAccountService.listAccounts();
        assertEquals(List.of("one", "two"), accounts.stream().map(BrokerAccountDto::getName).toList());
    }

    @Test
    void getById_found() {
        BrokerAccount account1 = new BrokerAccount();
        account1.setName("one");
        when(accountRepository.findById(1L))
                .thenReturn(Optional.of(account1));

        BrokerAccountDto accountDto = brokerAccountService.getById(1L);
        assertEquals("one", accountDto.getName());
    }

    @Test
    void getById_notFound() {
        BrokerEntityNotFoundException ex = assertThrows(BrokerEntityNotFoundException.class,
                () -> brokerAccountService.getById(1L));
        assertEquals("No brokerage account found with database id: 1", ex.getMessage());
    }

    @Test
    void addAccount_success() {
        CreateAccountCommand command = new CreateAccountCommand();
        command.setName("one");
        command.setOpenedDate(LocalDate.EPOCH);
        when(accountRepository.getOverlappingAccounts("one", LocalDate.EPOCH, null))
                .thenReturn(Collections.emptyList());

        BrokerAccountDto added = brokerAccountService.addAccount(command);
        assertEquals("one", added.getName());
        assertEquals(LocalDate.EPOCH, added.getOpenedDate());
        verify(accountRepository).save(argThat(a -> a.getOpenedDate().equals(LocalDate.EPOCH) && a.getName().equals("one")));
    }

    @Test
    void addAccount_overlaps() {
        CreateAccountCommand command = new CreateAccountCommand();
        command.setName("one");
        command.setOpenedDate(LocalDate.EPOCH);
        when(accountRepository.getOverlappingAccounts("one", LocalDate.EPOCH, null))
                .thenReturn(List.of(new DateRange(1L, LocalDate.EPOCH, LocalDate.of(2000, 1, 1))));

        UniqueViolationException ex = assertThrows(UniqueViolationException.class,
                () -> brokerAccountService.addAccount(command));
        assertEquals("The following accounts overlap with your request: [1970-01-01--2000-01-01 (id: 1)]",
                ex.getMessage());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void updateAccount_success() {
        UpdateAccountCommand command = new UpdateAccountCommand();
        command.setName("updated");
        command.setClosedDate(LocalDate.EPOCH);

        when(accountRepository.findById(1L))
                .thenReturn(Optional.of(new BrokerAccount()));
        when(accountRepository.getOtherOverlappingAccounts(1L, "updated", null, LocalDate.EPOCH))
                .thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> brokerAccountService.updateAccount(1L, command));
    }

    @Test
    void updateAccount_notFound() {
        UpdateAccountCommand command = new UpdateAccountCommand();
        command.setName("updated");
        command.setClosedDate(LocalDate.EPOCH);

        when(accountRepository.findById(1L))
                .thenReturn(Optional.empty());
        when(accountRepository.getOtherOverlappingAccounts(1L, "updated", null, LocalDate.EPOCH))
                .thenReturn(Collections.emptyList());

        BrokerEntityNotFoundException ex = assertThrows(BrokerEntityNotFoundException.class,
                () -> brokerAccountService.updateAccount(1L, command));
        assertEquals("No brokerage account found with database id: 1", ex.getMessage());
    }

    @Test
    void updateAccount_overlaps() {
        UpdateAccountCommand command = new UpdateAccountCommand();
        command.setName("updated");
        command.setClosedDate(LocalDate.EPOCH);

        when(accountRepository.getOtherOverlappingAccounts(1L, "updated", null, LocalDate.EPOCH))
                .thenReturn(List.of(new DateRange(10L, null, LocalDate.of(2000, 1, 1))));

        UniqueViolationException ex = assertThrows(UniqueViolationException.class,
                () -> brokerAccountService.updateAccount(1L, command));
        assertEquals("The following accounts overlap with your request: [--2000-01-01 (id: 10)]", ex.getMessage());
    }

    @Test
    void getReferenceById_mediates() {
        brokerAccountService.getReferenceById(1L);
        verify(accountRepository).getReferenceById(1L);
    }

    @Test
    void accountCountById_mediates() {
        when(accountRepository.countById(10L))
                .thenReturn(10);
        assertEquals(10, brokerAccountService.accountCountById(10L));
    }

    @Test
    void getMainAccountForAccount_found() {
        BrokerAccount thisAccount = new BrokerAccount();
        BrokerProduct product = new BrokerProduct();
        Broker broker = new Broker();
        broker.setId(22L);
        product.setBroker(broker);


        BrokerAccount main = new BrokerAccount();
        main.setId(33L);
        main.setAccountType(BrokerAccountType.MAIN);

        when(associationRepository.getAssociationOnDay(10L, LocalDate.EPOCH))
                .thenReturn(Optional.of(new ProductAssociation(thisAccount, product, null, LocalDate.EPOCH)));
        when(associationRepository.getMainAccountAssociation(22L, LocalDate.EPOCH))
                .thenReturn(Optional.of(new ProductAssociation(main, null, null, null)));

        BrokerAccount mainAccount = brokerAccountService.getMainAccountForAccount(10L, LocalDate.EPOCH);
        assertEquals(33L, mainAccount.getId());
        assertEquals(BrokerAccountType.MAIN, mainAccount.getAccountType());
    }

    @Test
    void getMainAccountForAccount_noAssociationForRequestedAccount() {
        BrokerProduct product = new BrokerProduct();
        Broker broker = new Broker();
        broker.setId(22L);
        product.setBroker(broker);


        BrokerAccount main = new BrokerAccount();
        main.setId(33L);
        main.setAccountType(BrokerAccountType.MAIN);

        when(associationRepository.getAssociationOnDay(10L, LocalDate.EPOCH))
                .thenReturn(Optional.empty());


        BrokerEntityNotFoundException ex = assertThrows(BrokerEntityNotFoundException.class,
                () -> brokerAccountService.getMainAccountForAccount(10L, LocalDate.EPOCH));
        assertEquals("No product association was found for account 10 on 1970-01-01", ex.getMessage());
    }

    @Test
    void getMainAccountForAccount_noMainAccountReturned() {
        BrokerAccount thisAccount = new BrokerAccount();
        BrokerProduct product = new BrokerProduct();
        Broker broker = new Broker();
        broker.setId(22L);
        product.setBroker(broker);


        BrokerAccount main = new BrokerAccount();
        main.setId(33L);
        main.setAccountType(BrokerAccountType.MAIN);

        when(associationRepository.getAssociationOnDay(10L, LocalDate.EPOCH))
                .thenReturn(Optional.of(new ProductAssociation(thisAccount, product, null, LocalDate.EPOCH)));
        when(associationRepository.getMainAccountAssociation(22L, LocalDate.EPOCH))
                .thenReturn(Optional.empty());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> brokerAccountService.getMainAccountForAccount(10L, LocalDate.EPOCH));
        assertEquals("No main account for account 10 on 1970-01-01", ex.getMessage());
    }

    @Test
    void getAccountHistory_found() {
        BrokerAccount queried = new BrokerAccount();
        queried.setName("queried");

        BrokerProduct product1 = new BrokerProduct();
        product1.setName("p1");
        product1.setBroker(new Broker("b1"));
        ProductAssociation association1 = new ProductAssociation(queried, product1, null, LocalDate.EPOCH);
        BrokerProduct product2 = new BrokerProduct();
        product2.setName("p2");
        product2.setBroker(new Broker("b2"));
        ProductAssociation association2 = new ProductAssociation(queried, product2, LocalDate.EPOCH.plusDays(1), null);

        when(accountRepository.findById(10L))
                .thenReturn(Optional.of(queried));
        when(associationRepository.getAccountHistory(10L))
                .thenReturn(List.of(association2, association1));

        AccountHistoryDto accountHistory = brokerAccountService.getAccountHistory(10L);
        assertEquals("queried", accountHistory.getAccountName());
        // history is sorted by date
        assertEquals("p1", accountHistory.getHistory().get(0).getProductName());
        assertEquals("b1", accountHistory.getHistory().get(0).getBrokerName());
        assertNull(accountHistory.getHistory().get(0).getFromDate());

        assertEquals("b2", accountHistory.getHistory().get(1).getBrokerName());
        assertEquals(LocalDate.EPOCH.plusDays(1), accountHistory.getHistory().get(1).getFromDate());
        assertNull(accountHistory.getHistory().get(1).getToDate());
    }

    @Test
    void getAccountHistory_accountNotFound() {
        BrokerAccount queried = new BrokerAccount();
        queried.setName("queried");

        when(accountRepository.findById(10L))
                .thenReturn(Optional.empty());

        BrokerEntityNotFoundException ex = assertThrows(BrokerEntityNotFoundException.class,
                () -> brokerAccountService.getAccountHistory(10L));
        assertEquals("No brokerage account found with database id: 10", ex.getMessage());
    }

    @Test
    void getAccountHistory_foundButNoHistory() {
        BrokerAccount queried = new BrokerAccount();
        queried.setName("queried");

        when(accountRepository.findById(10L))
                .thenReturn(Optional.of(queried));
        when(associationRepository.getAccountHistory(10L))
                .thenReturn(Collections.emptyList());

        AccountHistoryDto accountHistory = brokerAccountService.getAccountHistory(10L);
        assertEquals("queried", accountHistory.getAccountName());
        assertEquals(Collections.emptyList(), accountHistory.getHistory());
    }

    @Test
    void addProductToAccount_success() {
        CreateProductAssociationCommand command = new CreateProductAssociationCommand();
        command.setProductId(10L);
        command.setToDate(LocalDate.EPOCH);

        BrokerAccount account = new BrokerAccount();
        account.setId(11L);
        BrokerProduct product = new BrokerProduct();
        product.setId(10L);
        when(associationAdder.addItem(11L, 10L, command))
                .thenReturn(new ProductAssociation(account, product, null, LocalDate.EPOCH));

        ProductAssociationDto productAssociation = brokerAccountService.addProductToAccount(11L, command);
        assertEquals(11L, productAssociation.getAccountId());
        assertEquals(10L, productAssociation.getProductId());
        assertNull(productAssociation.getFromDate());
        assertEquals(LocalDate.EPOCH, productAssociation.getToDate());
    }
    // does not exist & overlap handled by adder


    @Test
    void updateAssociation_success() {
        UpdateProductAssociationCommand command = new UpdateProductAssociationCommand();
        command.setProductId(20L);
        command.setFromDate(LocalDate.EPOCH);

        assertDoesNotThrow(() -> brokerAccountService.updateAssociation(10L, 20L, command));
    }

    @Test
    void listAssociations() {
        BrokerAccount account = new BrokerAccount();
        account.setId(10L);
        BrokerProduct product1 = new BrokerProduct();
        product1.setId(1L);
        BrokerProduct product2 = new BrokerProduct();
        product2.setId(2L);
        ProductAssociation association1 = new ProductAssociation(account, product1, null, LocalDate.EPOCH);
        ProductAssociation association2 = new ProductAssociation(account, product2, LocalDate.EPOCH.plusDays(1), null);

        when(associationRepository.findAllByAccountId(10L))
                .thenReturn(List.of(association2, association1));

        List<ProductAssociationDto> associations = brokerAccountService.listAssociations(10L);
        // return list not sorted, returned as received from db
        assertEquals(List.of(2L, 1L), associations.stream().map(ProductAssociationDto::getProductId).toList());
        assertEquals(Stream.of(LocalDate.EPOCH.plusDays(1), null).toList(), associations.stream().map(ProductAssociationDto::getFromDate).toList());
    }

    @Test
    void getProductAssociation_found() {
        BrokerAccount account = new BrokerAccount();
        account.setId(10L);
        BrokerProduct product1 = new BrokerProduct();
        product1.setId(1L);
        ProductAssociation association1 = new ProductAssociation(account, product1, null, LocalDate.EPOCH);

        when(associationRepository.getAssociationOnDay(10L, LocalDate.EPOCH))
                .thenReturn(Optional.of(association1));

        ProductAssociation productAssociation = brokerAccountService.getProductAssociation(10L, LocalDate.EPOCH);
        assertEquals(1L, productAssociation.getProduct().getId());
        assertEquals(LocalDate.EPOCH, productAssociation.getToDate());
    }

    @Test
    void getProductAssociation_notFound() {
        BrokerEntityNotFoundException ex = assertThrows(BrokerEntityNotFoundException.class,
                () -> brokerAccountService.getProductAssociation(10L, LocalDate.EPOCH));
        assertEquals("No product association was found for account 10 on 1970-01-01", ex.getMessage());
    }
}