package app.broker.product.commission;

import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductCommissionMapper {
    List<ProductCommissionDto> toDto(List<ProductCommission> productCommissions);
}
