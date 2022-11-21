package app.analysis;

import app.data.DataService;
import app.util.InvalidDataException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvestmentArithmeticTest {

    @Mock
    DataService dataService;

    @InjectMocks
    InvestmentArithmetic investmentArithmetic;

    @Test
    void gettingReturnForValidFlow() {
        when(dataService.getExchangeRate("EUR", "EUR", LocalDate.of(2000, 1, 1)))
                .thenReturn(BigDecimal.ONE);
        when(dataService.getExchangeRate(eq("GBP"), eq("EUR"), any()))
                .thenReturn(new BigDecimal("2"));

        List<DatedCashValue> cashFlows = List.of(
                new DatedCashValue(BigDecimal.ONE.negate(), "EUR", LocalDate.of(2000, 1, 1)), // -1 EUR
                new DatedCashValue(BigDecimal.ONE.negate(), "GBP", LocalDate.of(2001, 1, 1)), // -2 EUR
                new DatedCashValue(new BigDecimal("2"), "GBP", LocalDate.of(2002, 1, 1))  // +4 EUR
        );

        double returnInEur = investmentArithmetic.getLifetimeReturnInPercent(cashFlows, "EUR");
        assertEquals(23.586975295415215, returnInEur);
        // 1.236^2 × -1 + 1.236^1 × -1 + 4 = 0
    }

    @Test
    void gettingReturnForOnlyNegativeOrZeroFlow() {
        when(dataService.getExchangeRate("EUR", "EUR", LocalDate.of(2000, 1, 1)))
                .thenReturn(BigDecimal.ONE);
        when(dataService.getExchangeRate(eq("GBP"), eq("EUR"), any()))
                .thenReturn(new BigDecimal("2"));

        List<DatedCashValue> cashFlows = List.of(
                new DatedCashValue(BigDecimal.ONE.negate(), "EUR", LocalDate.of(2000, 1, 1)),
                new DatedCashValue(BigDecimal.ZERO, "GBP", LocalDate.of(2001, 1, 1)),
                new DatedCashValue(new BigDecimal("2").negate(), "GBP", LocalDate.of(2002, 1, 1))
        );

        double returnInEur = investmentArithmetic.getLifetimeReturnInPercent(cashFlows, "EUR");
        assertEquals(-100, returnInEur);
    }

    @Test
    void gettingReturnForOnlyPositiveOrZeroFlow() {
        when(dataService.getExchangeRate("EUR", "EUR", LocalDate.of(2000, 1, 1)))
                .thenReturn(BigDecimal.ONE);
        when(dataService.getExchangeRate(eq("GBP"), eq("EUR"), any()))
                .thenReturn(new BigDecimal("2"));

        List<DatedCashValue> cashFlows = List.of(
                new DatedCashValue(BigDecimal.ONE, "EUR", LocalDate.of(2000, 1, 1)),
                new DatedCashValue(BigDecimal.ZERO, "GBP", LocalDate.of(2001, 1, 1)),
                new DatedCashValue(new BigDecimal("2"), "GBP", LocalDate.of(2002, 1, 1))
        );

        InvalidDataException ex = assertThrows(InvalidDataException.class,
                () -> investmentArithmetic.getLifetimeReturnInPercent(cashFlows, "EUR"));
        assertEquals("Return cannot be calculated", ex.getMessage());
    }

    @Test
    void gettingReturnForEmptyCashFlowList() {
        List<DatedCashValue> cashFlows = Collections.emptyList();

        InvalidDataException ex = assertThrows(InvalidDataException.class,
                () -> investmentArithmetic.getLifetimeReturnInPercent(cashFlows, "EUR"));
        assertEquals("Return cannot be calculated", ex.getMessage());
    }

}