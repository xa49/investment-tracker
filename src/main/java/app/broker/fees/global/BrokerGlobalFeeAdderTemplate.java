package app.broker.fees.global;

import app.broker.BrokerAdderUpdaterTemplate;
import app.broker.Broker;
import app.broker.BrokerRepository;
import app.broker.RequestCommand;
import app.broker.CommandLoadable;
import app.util.DateRange;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class BrokerGlobalFeeAdderTemplate extends BrokerAdderUpdaterTemplate {
    private final BrokerRepository brokerRepository;
    private final BrokerGlobalFeeRepository feeRepository;

    public BrokerGlobalFeeAdderTemplate(BrokerRepository brokerRepository, BrokerGlobalFeeRepository feeRepository) {
        super(brokerRepository, "Broker", feeRepository, "BrokerGlobalFee");
        this.brokerRepository = brokerRepository;
        this.feeRepository = feeRepository;
    }

    @Override
    protected int getAssociationCount(Long brokerId, Long feeId) {
        return feeRepository.countByBrokerIdAndId(brokerId, feeId);
    }

    @Override
    protected List<DateRange> getOverlappingPeriods(Long parentId, LocalDate from, LocalDate to, RequestCommand command) {
        return feeRepository.getOverlappingDates(parentId, from, to);
    }

    @Override
    protected List<DateRange> getOtherOverlappingPeriods(
            Long parentId, Long itemId, LocalDate from, LocalDate to, RequestCommand command) {
        return feeRepository.getOtherOverlappingDates(parentId, itemId, from, to);
    }

    @Override
    protected CommandLoadable getEmptyLoadableItem() {
        return new BrokerGlobalFee();
    }

    @Override
    protected void setParentReference(
            CommandLoadable item, Long parentId, LocalDate from, LocalDate to, RequestCommand command) {
        Broker broker = brokerRepository.getReferenceById(parentId);
        ((BrokerGlobalFee) item).setBroker(broker);
    }
}
