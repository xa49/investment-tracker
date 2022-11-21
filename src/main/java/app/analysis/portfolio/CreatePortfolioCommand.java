package app.analysis.portfolio;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CreatePortfolioCommand {
    private String name;
    private List<Long> accountIds;
}
