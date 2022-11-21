package app.broker;

import app.analysis.CashValue;
import app.broker.fees.calculator.FeeCalculatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Sql(scripts = {"classpath:/cleanbroker.sql", "classpath:/simplebroker.sql"})
class SimpleBrokerIT {

    @Autowired
    FeeCalculatorService feeCalculatorService;

    @Autowired
    BrokerService brokerService;

    BrokerDto broker;

    @BeforeEach
    void init() {
        broker = brokerService.getBrokerById(555L);
    }


    @Test
    void transferFeeCalculation() {
        CashValue investorCashFlow = feeCalculatorService.getTransferFee(555L, LocalDate.of(2022, 10, 4),
                new BigDecimal("10000"), "EUR");
        assertEquals("HUF", investorCashFlow.getCurrency());
        assertEquals(-18787, investorCashFlow.getAmount().intValue());
    }

    @Test
    void transferFeeAtMinimum() {
        CashValue investorCashFlow = feeCalculatorService.getTransferFee(555L, LocalDate.of(2022, 10, 4),
                new BigDecimal("1"), "EUR");
        assertEquals("HUF", investorCashFlow.getCurrency());
        assertEquals(-2710, investorCashFlow.getAmount().doubleValue(), 0.001);
    }

    @Test
    void transferFeeAtMaximum() {
        CashValue investorCashFlow = feeCalculatorService.getTransferFee(555L, LocalDate.of(2022, 10, 4),
                new BigDecimal("1000000000"), "EUR");
        assertEquals("HUF", investorCashFlow.getCurrency());
        assertEquals(-66_000, investorCashFlow.getAmount().doubleValue(), 0.001);
    }

    @Test
    void commissionWithinLimits() {
        CashValue commission = feeCalculatorService.getCommissionOnTransaction(555L, LocalDate.of(2022,10,4),
                "XETRA", new BigDecimal("10000"), "EUR");
        assertEquals(-35, commission.getAmount().doubleValue(), 0.0001);
        assertEquals("EUR", commission.getCurrency());

        commission = feeCalculatorService.getCommissionOnTransaction(555L, LocalDate.of(2022,10,4),
                "XETRA", new BigDecimal(1_000_000), "HUF");
        assertEquals("EUR", commission.getCurrency());
        assertEquals(-8.38323, commission.getAmount().doubleValue(), 0.001);
    }

    @Test
    void commissionBelowLimits() {
        CashValue commission = feeCalculatorService.getCommissionOnTransaction(555L, LocalDate.of(2022,10,4),
                "XETRA", new BigDecimal("10"), "EUR");
        assertEquals(-7, commission.getAmount().doubleValue(), 0.0001);
        assertEquals("EUR", commission.getCurrency());

        commission = feeCalculatorService.getCommissionOnTransaction(555L, LocalDate.of(2022,10,4),
                "XETRA", new BigDecimal(1_000), "HUF");
        assertEquals("EUR", commission.getCurrency());
        assertEquals(-7, commission.getAmount().doubleValue(), 0.001);
    }
}
