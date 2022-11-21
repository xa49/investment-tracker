package app.analysis.portfolio;

import lombok.Getter;

import java.util.List;

@Getter
public class UpdatePortfolioCommand {
    private String name;
    private List<Long> accountIds;
}
