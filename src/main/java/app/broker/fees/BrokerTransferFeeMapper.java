package app.broker.fees;

import app.broker.Broker;
import app.broker.fees.transfer.BrokerTransferFee;
import app.broker.fees.transfer.BrokerTransferFeeDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BrokerTransferFeeMapper {
    default Long map(Broker broker) {
        return broker.getId();
    }


    @Mapping(source = "broker", target = "brokerId")
    BrokerTransferFeeDto toDto(BrokerTransferFee brokerTransferFee);
    List<BrokerTransferFeeDto> toDto(List<BrokerTransferFee> brokerTransferFees);

}
