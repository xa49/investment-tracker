package app.broker.fees.transfer;

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
public class BrokerTransferFeeAdderTemplate extends BrokerAdderUpdaterTemplate {
    private final BrokerRepository brokerRepository;
    private final BrokerTransferFeeRepository feeRepository;

    public BrokerTransferFeeAdderTemplate(BrokerRepository brokerRepository, BrokerTransferFeeRepository feeRepository) {
        super(brokerRepository, "Broker", feeRepository, "BrokerTransferFee");
        this.brokerRepository = brokerRepository;
        this.feeRepository = feeRepository;
    }



    @Override
    protected List<DateRange> getOverlappingPeriods(Long parentId, LocalDate from, LocalDate to, RequestCommand command) {
        return feeRepository.getOverlappingDates(parentId, ((TransferFeeCommand) command).getTransferredCurrency(), from, to);
    }

    @Override
    protected List<DateRange> getOtherOverlappingPeriods(
            Long parentId, Long itemId, LocalDate from, LocalDate to, RequestCommand command) {
        return  feeRepository.getOtherOverlappingDates(
                parentId, itemId, ((TransferFeeCommand) command).getTransferredCurrency(), from, to);
    }

    @Override
    protected CommandLoadable getEmptyLoadableItem() {
        return new BrokerTransferFee();
    }

    @Override
    protected void setParentReference(
            CommandLoadable item, Long parentId, LocalDate from, LocalDate to, RequestCommand command) {
        Broker broker = brokerRepository.getReferenceById(parentId);
        ((BrokerTransferFee) item).setBroker(broker);
    }

    @Override
    protected int getAssociationCount(Long parentId, Long itemId) {
        return feeRepository.countByBrokerIdAndId(parentId, itemId);
    }
}
