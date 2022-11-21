package app.broker.product;

import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BrokerProductMapper {
    List<BrokerProductDto> toDto(List<BrokerProduct> brokerProducts);
}
