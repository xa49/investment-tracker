package app.analysis;

import app.analysis.actual.ActualPositionOverviewDto;
import app.analysis.actual.ActualPositionService;
import app.analysis.actual.GetActualPositionCommand;
import app.analysis.liquid.GetLiquidValueCommand;
import app.analysis.liquid.LiquidValueDto;
import app.analysis.portfolio.CreatePortfolioCommand;
import app.analysis.portfolio.PortfolioDto;
import app.analysis.portfolio.PortfolioService;
import app.analysis.portfolio.UpdatePortfolioCommand;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analysis")
@AllArgsConstructor
@Slf4j
public class InvestmentAnalysisController {

    private final InvestmentAnalysisService investmentAnalysisService;

    private final PortfolioService portfolioService;

    private final ActualPositionService actualPositionService;

    @GetMapping
    public ResponseEntity<LiquidValueDto> getLiquidValue(@Valid @RequestBody GetLiquidValueCommand command) {
        return ResponseEntity.ok(investmentAnalysisService.getLiquidValue(command.getPortfolioName(),
                command.getTaxResidence(), command.getCurrency(), command.getAsOfDate()));
    }

    @GetMapping("/actual-position")
    public ResponseEntity<ActualPositionOverviewDto> getActualPosition(
            @Valid @RequestBody GetActualPositionCommand command) {
        return ResponseEntity.ok(
                actualPositionService.getActualPositionOverview(
                        command.getPortfolioName(), command.getTaxResidence(), command.getAsOfDate()));
    }

    // PORTFOLIO - no checks on the existence of associated brokerage accounts
    @GetMapping("/portfolio")
    public ResponseEntity<List<PortfolioDto>> listPortfolios() {
        return ResponseEntity.ok(portfolioService.listPortfolios());
    }

    @PostMapping("/portfolio")
    public ResponseEntity<PortfolioDto> addPortfolio(@Valid @RequestBody CreatePortfolioCommand command) {
        return ResponseEntity.created(URI.create("portfolio"))
                .body(portfolioService.addPortfolio(command));
    }

    @PutMapping("/portfolio/{portfolioId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updatePortfolio(@PathVariable("portfolioId") Long portfolioId,
                                @Valid @RequestBody UpdatePortfolioCommand command) {
        portfolioService.updatePortfolio(portfolioId, command);
    }
}
