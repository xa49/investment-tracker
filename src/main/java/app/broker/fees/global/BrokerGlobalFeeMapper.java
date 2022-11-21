package app.broker.fees.global;

import app.broker.Broker;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BrokerGlobalFeeMapper {
    default Long map(Broker broker) {
        return broker.getId();
    }

    @Mapping(source = "broker", target = "brokerId")
    BrokerGlobalFeeDto toDto(BrokerGlobalFee brokerGlobalFee);

    List<BrokerGlobalFeeDto> toDto(List<BrokerGlobalFee> globalFees);

}
