package app.broker.account.association;

import app.broker.BrokerEntityNotFoundException;
import app.broker.OverlappingDetail;
import app.broker.UniqueViolationException;
import app.broker.account.BrokerAccount;
import app.broker.account.BrokerAccountRepository;
import app.broker.product.BrokerProduct;
import app.broker.product.BrokerProductRepository;
import app.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
public class ProductAssociationAdder {

    private final BrokerProductRepository productRepository;
    private final BrokerAccountRepository accountRepository;
    private final ProductAssociationRepository associationRepository;

    public ProductAssociationAdder(BrokerProductRepository productRepository, BrokerAccountRepository accountRepository,
                                   ProductAssociationRepository associationRepository) {
        this.productRepository = productRepository;
        this.accountRepository = accountRepository;
        this.associationRepository = associationRepository;
    }

    public ProductAssociation addItem(Long accountId, Long productId, ProductAssociationCommand command) {
        validateAccount(accountId);
        validateProduct(productId);

        List<DateRange> overlappingPeriods =
                associationRepository.getOverlappingDates(accountId, productId, command.getFromDate(), command.getToDate());

        if (!overlappingPeriods.isEmpty()) {
            OverlappingDetail overlappingDetail =
                    constructOverlappingDetail(command.getFromDate(), command.getToDate(), overlappingPeriods);
            throw new IllegalArgumentException("AccountProductAssociation: " + overlappingDetail.getMessage());
        }

        BrokerProduct product = productRepository.getReferenceById(productId);
        BrokerAccount account = accountRepository.getReferenceById(accountId);
        ProductAssociation item = new ProductAssociation(account, product, command.getFromDate(), command.getToDate());
        associationRepository.save(item);
        log.debug("AccountProductAssociation added: {}. Command was: {}", item, command);
        return item;
    }

    public void updateItem(Long accountId, Long productId, Long associationId, ProductAssociationCommand command) {
        validateAccount(accountId);
        validateProduct(productId);

        List<DateRange> overlappingPeriods =
                associationRepository.getOtherOverlappingDates(
                        accountId, productId, associationId, command.getFromDate(), command.getToDate());

        if (!overlappingPeriods.isEmpty()) {
            OverlappingDetail overlappingDetail =
                    constructOverlappingDetail(command.getFromDate(), command.getToDate(), overlappingPeriods);
            throw new UniqueViolationException("AccountProductAssociation:" + overlappingDetail.getMessage());
        }

        BrokerProduct product = productRepository.getReferenceById(productId);
        BrokerAccount account = accountRepository.getReferenceById(accountId);
        ProductAssociation item = associationRepository.findById(associationId)
                .orElseThrow(() -> new BrokerEntityNotFoundException("Association not found with id: " + associationId));
        item.setProduct(product);
        item.setAccount(account);
        item.setFromDate(command.getFromDate());
        item.setToDate(command.getToDate());
        log.debug("AccountProductAssociation was updated: {}. Command was: {}", item, command);
    }

    private void validateProduct(Long productId) {
        if(productRepository.countById(productId) == 0) {
            throw new IllegalArgumentException("BrokerProduct not found with id: " + productId);
        }
    }

    private void validateAccount(Long accountId) {
        if(accountRepository.countById(accountId) == 0) {
            throw new IllegalArgumentException("BrokerAccount not found with id: " + accountId);
        }
    }

    private OverlappingDetail constructOverlappingDetail(
            LocalDate requestFrom, LocalDate requestTo, List<DateRange> issues) {
        return new OverlappingDetail(requestFrom, requestTo, issues);
    }
}
