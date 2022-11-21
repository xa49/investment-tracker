package app.analysis.portfolio;

import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PortfolioMapper {
    PortfolioDto toDto(Portfolio portfolio);

    List<PortfolioDto> toDto(List<Portfolio> portfolios);
}
