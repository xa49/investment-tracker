package app.taxation.details;

import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TaxDetailMapper {
    TaxDetailsDto toDto(TaxDetails taxDetails);

    List<TaxDetailsDto> toDto(List<TaxDetails> taxDetails);
}
