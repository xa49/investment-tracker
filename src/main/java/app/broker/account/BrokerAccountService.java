package app.broker.account;

import app.broker.BrokerEntityNotFoundException;
import app.broker.UniqueViolationException;
import app.broker.account.association.*;
import app.util.DateRange;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class BrokerAccountService {

    private static final String ACCOUNT_NOT_FOUND_EXCEPTION_MESSAGE = "No brokerage account found with database id: ";
    private final BrokerAccountRepository accountRepository;
    private final BrokerAccountMapper accountMapper;
    private final ProductAssociationMapper associationMapper;
    private final ProductAssociationRepository associationRepository;
    private final ProductAssociationAdder associationAdder;

    public List<BrokerAccountDto> listAccounts() {
        return accountMapper.toDto(accountRepository.findAll());
    }

    public BrokerAccountDto getById(Long accountId) {
        return accountMapper.toDto(
                accountRepository.findById(accountId)
                        .orElseThrow(
                                () -> new BrokerEntityNotFoundException(ACCOUNT_NOT_FOUND_EXCEPTION_MESSAGE + accountId)));
    }

    public BrokerAccountDto addAccount(CreateAccountCommand command) {
        List<DateRange> overlappingAccounts =
                accountRepository.getOverlappingAccounts(command.getName(), command.getOpenedDate(), command.getClosedDate());
        if (overlappingAccounts.isEmpty()) {
            BrokerAccount newAccount = new BrokerAccount();
            newAccount.loadFromCommand(command);
            accountRepository.save(newAccount);
            return accountMapper.toDto(newAccount);
        }
        throw new UniqueViolationException("The following accounts overlap with your request: "
                + overlappingAccounts.stream().map(DateRange::toString).collect(Collectors.toSet()));
    }

    @Transactional
    public void updateAccount(Long accountId, UpdateAccountCommand command) {
        List<DateRange> overlappingAccounts =
                accountRepository.getOtherOverlappingAccounts(accountId, command.getName(), command.getOpenedDate(), command.getClosedDate());
        if (overlappingAccounts.isEmpty()) {
            BrokerAccount account = accountRepository.findById(accountId)
                    .orElseThrow(() -> new BrokerEntityNotFoundException(ACCOUNT_NOT_FOUND_EXCEPTION_MESSAGE + accountId));
            account.loadFromCommand(command);
        } else {
            throw new UniqueViolationException("The following accounts overlap with your request: "
                    + overlappingAccounts.stream().map(DateRange::toString).collect(Collectors.toSet()));
        }
    }

    public BrokerAccount getReferenceById(Long accountId) {
        return accountRepository.getReferenceById(accountId);
    }

    public int accountCountById(Long accountId) {
        return accountRepository.countById(accountId);
    }

    public BrokerAccount getMainAccountForAccount(Long accountId, LocalDate date) {
        ProductAssociation queriedAssociation = getProductAssociation(accountId, date);
        Long brokerId = queriedAssociation.getProduct().getBroker().getId();

        return associationRepository.getMainAccountAssociation(brokerId, date)
                .map(ProductAssociation::getAccount)
                .orElseThrow(() -> new IllegalStateException("No main account for account " + accountId + " on " + date));
    }

    public AccountHistoryDto getAccountHistory(Long accountId) {
        BrokerAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new BrokerEntityNotFoundException(ACCOUNT_NOT_FOUND_EXCEPTION_MESSAGE + accountId));
        List<ProductAssociation> associations = associationRepository.getAccountHistory(accountId);
        List<AccountHistoryDto.AssociationDetailDto> history = associations.stream()
                .map(this::getAssociationDetails)
                .sorted(Comparator.comparing(AccountHistoryDto.AssociationDetailDto::getFromDate,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .toList();
        return new AccountHistoryDto(account.getName(), history);
    }

    private AccountHistoryDto.AssociationDetailDto getAssociationDetails(ProductAssociation association) {
        return new AccountHistoryDto.AssociationDetailDto(association.getId(), association.getProduct().getName(),
                association.getProduct().getBroker().getName(), association.getFromDate(), association.getToDate());
    }

    public ProductAssociationDto addProductToAccount(Long accountId, CreateProductAssociationCommand command) {
        return associationMapper.toDto(associationAdder.addItem(accountId, command.getProductId(), command));
    }

    @Transactional
    public void updateAssociation(Long accountId, Long associationId, UpdateProductAssociationCommand command) {
        associationAdder.updateItem(accountId, command.getProductId(), associationId, command);
    }

    public List<ProductAssociationDto> listAssociations(Long accountId) {
        return associationMapper.toDto(associationRepository.findAllByAccountId(accountId));
    }

    public ProductAssociation getProductAssociation(Long accountId, LocalDate date) {
        Optional<ProductAssociation> association = associationRepository.getAssociationOnDay(accountId, date);
        if (association.isPresent()) {
            return association.get();
        }
        throw new BrokerEntityNotFoundException("No product association was found for account " + accountId + " on " + date);
    }
}
