package app.analysis;

import app.analysis.liquid.LiquidValueDto;
import app.analysis.liquid.LiquidValueOrchestrator;
import app.analysis.portfolio.PortfolioService;
import app.analysis.tracker.ActualPositionTracker;
import app.analysis.tracker.LiquidPositionTracker;
import app.util.InvalidDataException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvestmentAnalysisServiceTest {

    @Mock
    PortfolioService portfolioService;

    @Mock
    InvestmentArithmetic investmentArithmetic;

    @Mock
    LiquidValueOrchestrator liquidValueOrchestrator;

    @InjectMocks
    InvestmentAnalysisService investmentAnalysisService;

    @Test
    void gettingLiquidValueAnalysisForPortfolio() {
        when(portfolioService.getAccountIdsInPortfolio("portfolio"))
                .thenReturn(Set.of(1L, 2L, 3L));
        when(liquidValueOrchestrator.getLiquidPositionTracker(argThat(l -> new HashSet<>(l).equals(Set.of(1L, 2L, 3L))),
                eq("HU"), eq(LocalDate.EPOCH), eq("GBP")))
                .thenReturn(basicLiquidPositionTracker());
        when(investmentArithmetic.getLifetimeReturnInPercent(basicLiquidPositionTracker().getReturnCashFlows(), "GBP"))
                .thenReturn(10.234);

        LiquidValueDto liquidValueDto =
                investmentAnalysisService.getLiquidValue("portfolio", "HU", "GBP", LocalDate.EPOCH);

        assertEquals(new CashValue(BigDecimal.TEN, "GBP"), liquidValueDto.getFullyLiquidValue());
        assertEquals(10.234, liquidValueDto.getInvestmentReturnInPercent());
    }

    @Test
    void gettingLiquidValueWhenNotEnoughTransactions() {
        when(portfolioService.getAccountIdsInPortfolio("portfolio"))
                .thenReturn(Set.of(1L, 2L, 3L));
        when(liquidValueOrchestrator.getLiquidPositionTracker(argThat(l -> new HashSet<>(l).equals(Set.of(1L, 2L, 3L))),
                eq("HU"), eq(LocalDate.EPOCH), eq("GBP")))
                .thenReturn(basicLiquidPositionTracker());
        when(investmentArithmetic.getLifetimeReturnInPercent(basicLiquidPositionTracker().getReturnCashFlows(), "GBP"))
                .thenThrow(new InvalidDataException("Return cannot be calculated"));

        LiquidValueDto liquidValueDto =
                investmentAnalysisService.getLiquidValue("portfolio", "HU", "GBP", LocalDate.EPOCH);
        assertEquals(new CashValue(BigDecimal.TEN, "GBP"), liquidValueDto.getFullyLiquidValue());
        assertNull(liquidValueDto.getInvestmentReturnInPercent());
    }

    public LiquidPositionTracker basicLiquidPositionTracker() {
        LiquidPositionTracker liquidPositionTracker = new LiquidPositionTracker(ActualPositionTracker.getBlank(), LocalDate.EPOCH, "GBP");
        liquidPositionTracker.setLiquidValue(new CashValue(BigDecimal.TEN, "GBP"));
        return liquidPositionTracker;
    }

}