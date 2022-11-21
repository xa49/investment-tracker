package app.broker.account.association;

import app.broker.Broker;
import app.broker.account.BrokerAccount;
import app.broker.product.BrokerProduct;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductAssociationMapper {
    List<ProductAssociationDto> toDto(List<ProductAssociation> associations);

    default Long map(Broker broker) {
        return broker.getId();
    }

    default Long map(BrokerAccount account) {
        return account.getId();
    }

    default Long map(BrokerProduct product) {
        return product.getId();
    }

    @Mapping(source = "account", target = "accountId")
    @Mapping(source = "product", target = "productId")
    ProductAssociationDto toDto(ProductAssociation productAssociation);
}
