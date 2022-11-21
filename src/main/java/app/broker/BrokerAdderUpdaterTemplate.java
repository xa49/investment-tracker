package app.broker;

import app.util.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Getter
@Setter
public abstract class BrokerAdderUpdaterTemplate {

    private HasCountById parentRepository;
    private String parentName;
    private JpaRepository itemRepo;
    private String itemName;

    protected BrokerAdderUpdaterTemplate(
            HasCountById parentRepository, String parentName, JpaRepository itemRepo, String itemName) {
        this.parentRepository = parentRepository;
        this.parentName = parentName;
        this.itemRepo = itemRepo;
        this.itemName = itemName;
    }

    public final CommandLoadable addItem(Long parentId, LocalDate from, LocalDate to, RequestCommand command) {
        validateParentExists(parentId);
        customValidation();

        List<DateRange> overlappingPeriods = getOverlappingPeriods(parentId, from, to, command);
        if (!overlappingPeriods.isEmpty()) {
            OverlappingDetail overlappingDetail = constructOverlappingDetail(from, to, overlappingPeriods);
            throw new UniqueViolationException(itemName + ": " + overlappingDetail.getMessage());
        }

        CommandLoadable item = getEmptyLoadableItem();
        item.loadFromCommand(command);
        setParentReference(item, parentId, from, to, command);

        itemRepo.save(item);
        log.debug("{} added: {}. Command was: {}", itemName, item, command);
        return item;
    }


    public final CommandLoadable updateItem(
            Long parentId, Long itemId, LocalDate from, LocalDate to, RequestCommand command) {
        validateParentExists(parentId);
        validateAssociationWithParentExists(parentId, itemId);
        customValidation();

        List<DateRange> overlappingPeriods = getOtherOverlappingPeriods(parentId, itemId, from, to, command);
        if (!overlappingPeriods.isEmpty()) {
            OverlappingDetail overlappingDetail = constructOverlappingDetail(from, to, overlappingPeriods);
            throw new UniqueViolationException(itemName + ": " + overlappingDetail.getMessage());
        }

        CommandLoadable item = getExistingItem(itemId);
        item.loadFromCommand(command);
        log.debug("{} updated: {}. Command was: {}", itemName, item, command);
        return item;
    }

    private void validateParentExists(Long id) {
        if (parentRepository.countById(id) == 0) {
            throw new BrokerEntityNotFoundException(parentName + " not found with id: " + id);
        }
    }

    private void validateAssociationWithParentExists(Long parentId, Long itemId) {
        int count = getAssociationCount(parentId, itemId);
        System.out.println("association count between " + parentId + " " + itemId + " " + count);
        if (count == 0) {
            throw new BrokerEntityNotFoundException("No " + itemName + " with id: " + itemId
                    + " linked to " + parentName + " with id: " + parentId);
        }
    }

    protected abstract int getAssociationCount(Long parentId, Long itemId);

    protected void customValidation() {
        // for another parent validation
    }

    protected abstract List<DateRange> getOverlappingPeriods(
            Long parentId, LocalDate from, LocalDate to, RequestCommand command);

    protected abstract List<DateRange> getOtherOverlappingPeriods(
            Long parentId, Long itemId, LocalDate from, LocalDate to, RequestCommand command);

    private OverlappingDetail constructOverlappingDetail(
            LocalDate requestFrom, LocalDate requestTo, List<DateRange> issues) {
        return new OverlappingDetail(requestFrom, requestTo, issues);
    }

    protected abstract CommandLoadable getEmptyLoadableItem();

    private CommandLoadable getExistingItem(Long itemId) {
        Optional<CommandLoadable> item = itemRepo.findById(itemId);
        if(item.isPresent()) {
            return item.get();
        } else {
            throw new IllegalStateException("Already validated that " + getItemName()
                    + " is present but query found none for id " + itemId);
        }
    }

    protected abstract void setParentReference(
            CommandLoadable item, Long parentId, LocalDate from, LocalDate to, RequestCommand command);

}
