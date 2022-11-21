package app.analysis.portfolio;

import lombok.Data;

import java.util.List;

@Data
public class PortfolioDto {
    private Long id;
    private String name;
    private List<Long> accountIds;
}
