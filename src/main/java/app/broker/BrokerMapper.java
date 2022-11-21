package app.broker;

import app.broker.account.BrokerAccount;
import app.broker.product.BrokerProduct;
import app.broker.product.BrokerProductDto;
import app.broker.product.commission.ProductCommission;
import app.broker.product.commission.ProductCommissionDto;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BrokerMapper {

    default Long map(Broker broker) {
        return broker.getId();
    }

    default Long map(BrokerAccount account) {
        return account.getId();
    }

    default Long map(BrokerProduct product) {
        return product.getId();
    }

    BrokerDto toDto(Broker broker);

    List<BrokerDto> toDto(List<Broker> brokers);

    BrokerProductDto toDto(BrokerProduct brokerProduct);

    ProductCommissionDto toDto(ProductCommission productCommission);

}
