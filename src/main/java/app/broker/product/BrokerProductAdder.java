package app.broker.product;

import app.broker.BrokerAdderUpdaterTemplate;
import app.broker.Broker;
import app.broker.BrokerRepository;
import app.broker.CommandLoadable;
import app.util.DateRange;
import app.broker.RequestCommand;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class BrokerProductAdder extends BrokerAdderUpdaterTemplate {
    private final BrokerRepository brokerRepository;
    private final BrokerProductRepository productRepository;

    public BrokerProductAdder(BrokerRepository brokerRepository, BrokerProductRepository productRepository) {
        super(brokerRepository, "Broker", productRepository, "BrokerProduct");
        this.brokerRepository = brokerRepository;
        this.productRepository = productRepository;
    }

    @Override
    protected int getAssociationCount(Long parentId, Long itemId) {
        return productRepository.countByBrokerIdAndId(parentId, itemId);
    }

    @Override
    protected List<DateRange> getOverlappingPeriods(
            Long parentId, LocalDate from, LocalDate to, RequestCommand command) {
        return productRepository.getOverlappingDates(parentId, ((ProductCommand) command).getName(), from, to);
    }

    @Override
    protected List<DateRange> getOtherOverlappingPeriods(
            Long parentId, Long itemId, LocalDate from, LocalDate to, RequestCommand command) {
        return productRepository.getOtherOverlappingDates(parentId, itemId, ((ProductCommand) command).getName(), from, to);

    }

    @Override
    protected CommandLoadable getEmptyLoadableItem() {
        return new BrokerProduct();
    }

    @Override
    protected void setParentReference(
            CommandLoadable item, Long parentId, LocalDate from, LocalDate to, RequestCommand command) {
        Broker broker = brokerRepository.getReferenceById(parentId);
        ((BrokerProduct) item).setBroker(broker);
    }
}
