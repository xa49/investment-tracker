package app.analysis;

import app.analysis.liquid.LiquidValueDto;
import app.analysis.liquid.LiquidValueOrchestrator;
import app.analysis.portfolio.PortfolioService;
import app.analysis.tracker.LiquidPositionTracker;
import app.util.InvalidDataException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@AllArgsConstructor
@Slf4j
public class InvestmentAnalysisService {

    private final PortfolioService portfolioService;
    private final InvestmentArithmetic investmentArithmetic;
    private final LiquidValueOrchestrator liquidValueOrchestrator;

    public LiquidValueDto getLiquidValue(
            String portfolioName, String taxResidence, String currency, LocalDate asOfDate) {
        Set<Long> accountIds = portfolioService.getAccountIdsInPortfolio(portfolioName);
        return getLiquidValue(new ArrayList<>(accountIds), taxResidence, currency, asOfDate);
    }

    public LiquidValueDto getLiquidValue(
            List<Long> accountIds, String taxResidence, String currency, LocalDate asOfDate) {
        LiquidPositionTracker liquidPositionTracker =
                liquidValueOrchestrator.getLiquidPositionTracker(accountIds, taxResidence, asOfDate, currency);

        try { // must have at least two transactions, otherwise IAE
            liquidPositionTracker.setInvestmentReturnInPercent(
                    investmentArithmetic.getLifetimeReturnInPercent(
                            liquidPositionTracker.getReturnCashFlows(), currency));
        } catch (InvalidDataException e) {
            e.printStackTrace();
            liquidPositionTracker.setInvestmentReturnInPercent(null);
        }
        liquidPositionTracker.compressContents();
        return liquidPositionTracker.getLiquidValueDto();
    }

}
