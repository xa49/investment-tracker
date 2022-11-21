package app.analysis.liquid;

import app.analysis.CashValue;
import app.analysis.tracker.ActualPositionTracker;
import app.analysis.tracker.LiquidPositionTracker;
import app.data.DataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CashFlowConverterTest {


    @Mock
    DataService dataService;

    @InjectMocks
    CashFlowConverter cashFlowConverter;

    @Test
    void convertingBalances() {
        when(dataService.getExchangeRate("EUR", "USD", LocalDate.EPOCH))
                .thenReturn(new BigDecimal(5));
        when(dataService.getExchangeRate("HUF", "USD", LocalDate.EPOCH))
                .thenReturn(new BigDecimal(2));
        when(dataService.getExchangeRate("GBP", "USD", LocalDate.EPOCH))
                .thenReturn(new BigDecimal(3));

        CashValue converted = cashFlowConverter.convertAll(
                Map.of("EUR", BigDecimal.TEN,
                        "HUF", BigDecimal.TEN,
                        "GBP", BigDecimal.ONE),
                new LiquidPositionTracker(ActualPositionTracker.getBlank(), LocalDate.EPOCH, "USD"));

        // 73 x hard coded 0.5% fee
        assertEquals(CashValue.of(new BigDecimal("72.635"), "USD"), converted);
    }

}