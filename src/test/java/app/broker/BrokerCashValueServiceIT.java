package app.broker;

import app.broker.fees.BrokerFeeService;
import app.broker.fees.global.CreateBrokerGlobalFeeCommand;
import app.broker.fees.global.UpdateBrokerGlobalFeeCommand;
import app.broker.fees.transfer.CreateBrokerTransferFeeCommand;
import app.broker.fees.transfer.UpdateBrokerTransferFeeCommand;
import app.broker.fees.global.BrokerGlobalFeeDto;
import app.broker.fees.transfer.BrokerTransferFeeDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Sql(scripts = "classpath:/cleanbroker.sql")
class BrokerCashValueServiceIT {

    @Autowired
    BrokerFeeService brokerFeeService;

    @Autowired
    BrokerService brokerService;

    @Test
    void addValidBrokerTransferFee() {
        BrokerDto brokerDto = addBrokerNamedBroker();

        CreateBrokerTransferFeeCommand command = new CreateBrokerTransferFeeCommand();
        command.setFeeCurrency("EUR");
        command.setFromDate(LocalDate.of(2000, 1, 1));
        command.setToDate(LocalDate.of(2004, 1, 1));

        BrokerTransferFeeDto dto = brokerFeeService.addTransferFee(brokerDto.getId(), command);
        assertEquals("EUR", dto.getFeeCurrency());
    }

    @Test
    void addingFeeDetailsForOverlappingPeriodAllDatesDefined() {
        BrokerDto brokerDto = addBrokerNamedBroker();

        CreateBrokerTransferFeeCommand command = new CreateBrokerTransferFeeCommand();
        command.setFeeCurrency("EUR");
        command.setFromDate(LocalDate.of(2000, 1, 1));
        command.setToDate(LocalDate.of(2004, 1, 1));
        Long id = brokerDto.getId();
        brokerFeeService.addTransferFee(id, command);

        // Overlap with previous
        command.setFromDate(LocalDate.of(2001, 1, 1));
        command.setToDate(LocalDate.of(2001, 1, 5));

        UniqueViolationException ex = assertThrows(UniqueViolationException.class, () -> brokerFeeService.addTransferFee(id, command));
        assertThat(ex.getMessage(), startsWith("BrokerTransferFee: You provided the range 2001-01-01 - 2001-01-05 but these periods overlap with it: [2000-01-01--2004-01-01 (id: "));
    }

    @Test
    void addingFeeDetailsForOverLappingPeriodExistingHaveUndefined() {
        BrokerDto brokerDto = addBrokerNamedBroker();

        CreateBrokerTransferFeeCommand command = new CreateBrokerTransferFeeCommand();
        command.setFeeCurrency("EUR");
        command.setFromDate(LocalDate.of(2000, 1, 1));
        Long id = brokerDto.getId();
        brokerFeeService.addTransferFee(id, command); // From 2000-01-01 indefinitely

        command.setFromDate(null);
        command.setToDate(LocalDate.of(1999, 10, 1)); // All the way until 1999-10-01
        brokerFeeService.addTransferFee(id, command);

        command.setFromDate(LocalDate.of(1998, 1, 1));
        command.setToDate(LocalDate.of(1999, 1, 1));
        UniqueViolationException ex = assertThrows(UniqueViolationException.class, () -> brokerFeeService.addTransferFee(id, command));
        assertThat(ex.getMessage(), startsWith("BrokerTransferFee: You provided the range 1998-01-01 - 1999-01-01 but these periods overlap with it: [--1999-10-01 (id: "));


        command.setFromDate(LocalDate.of(2002, 1, 1));
        command.setToDate(LocalDate.of(2008, 1, 1));
        ex = assertThrows(UniqueViolationException.class, () -> brokerFeeService.addTransferFee(id, command));
        assertThat(ex.getMessage(), startsWith("BrokerTransferFee: You provided the range 2002-01-01 - 2008-01-01 but these periods overlap with it: [2000-01-01-- (id: "));
    }

    @Test
    void addingFeeDetailsForOverlappingPeriodNewHaveUndefined() {
        BrokerDto brokerDto = addBrokerNamedBroker();

        CreateBrokerTransferFeeCommand command = new CreateBrokerTransferFeeCommand();
        command.setFeeCurrency("EUR");
        command.setFromDate(LocalDate.of(2000, 1, 1));
        command.setToDate(LocalDate.of(2002, 1, 1));
        Long id = brokerDto.getId();
        brokerFeeService.addTransferFee(id, command);

        command.setFromDate(null);
        command.setToDate(LocalDate.of(2001, 1, 1));
        UniqueViolationException ex = assertThrows(UniqueViolationException.class, () -> brokerFeeService.addTransferFee(id, command));
        assertThat(ex.getMessage(), startsWith("BrokerTransferFee: You provided the range  - 2001-01-01 but these periods overlap with it: [2000-01-01--2002-01-01 (id: "));


        command.setFromDate(LocalDate.of(2001, 1, 1));
        ex = assertThrows(UniqueViolationException.class, () -> brokerFeeService.addTransferFee(id, command));
        assertThat(ex.getMessage(), startsWith("BrokerTransferFee: You provided the range 2001-01-01 - 2001-01-01 but these periods overlap with it: [2000-01-01--2002-01-01 (id: "));

    }

    @Test
    void overlapAllowedForDifferentTransferCurrency() {
        BrokerDto brokerDto = addBrokerNamedBroker();

        CreateBrokerTransferFeeCommand command = new CreateBrokerTransferFeeCommand();
        command.setTransferredCurrency("EUR");
        command.setFromDate(LocalDate.of(2000, 1, 1));
        command.setToDate(LocalDate.of(2004, 1, 1));
        Long id = brokerDto.getId();
        brokerFeeService.addTransferFee(id, command);

        // Overlap with previous but this is for USD
        command.setFromDate(LocalDate.of(2001, 1, 1));
        command.setToDate(LocalDate.of(2001, 1, 5));
        command.setTransferredCurrency("USD");

        BrokerTransferFeeDto dto = brokerFeeService.addTransferFee(id, command);
        assertEquals("USD", dto.getTransferredCurrency());
    }

    @Test
    void addingTransferFeeDetailsForNotExistingBroker() {
        CreateBrokerTransferFeeCommand command = new CreateBrokerTransferFeeCommand();
        command.setFeeCurrency("EUR");

        BrokerEntityNotFoundException ex = assertThrows(BrokerEntityNotFoundException.class,
                () -> brokerFeeService.addTransferFee(1L, command));
        assertEquals("Broker not found with id: 1", ex.getMessage());
    }

    @Test
    void updatingTransferFeeDetailsWithValidDetails() {
        BrokerDto brokerDto = addBrokerNamedBroker();

        CreateBrokerTransferFeeCommand createCommand = new CreateBrokerTransferFeeCommand();
        createCommand.setFromDate(LocalDate.of(2000, 1, 1));
        createCommand.setToDate(LocalDate.of(2001, 1, 1));

        BrokerTransferFeeDto feeDto = brokerFeeService.addTransferFee(brokerDto.getId(), createCommand);

        UpdateBrokerTransferFeeCommand updateCommand = new UpdateBrokerTransferFeeCommand();
        updateCommand.setMaximumFee(new BigDecimal("210"));
        updateCommand.setFeeCurrency("USD");
        updateCommand.setFromDate(LocalDate.of(2000,6,1));
        brokerFeeService.updateTransferFee(brokerDto.getId(), feeDto.getId(), updateCommand);

        BrokerTransferFeeDto queried = brokerFeeService.getTransferFeeById(feeDto.getId());
        assertEquals(LocalDate.of(2000, 6, 1), queried.getFromDate());
        assertEquals(new BigDecimal("210"), queried.getMaximumFee());
        assertEquals("USD", queried.getFeeCurrency());
    }

    @Test
    void updatingTransferFeeDetailsForInvalidId() {
        BrokerDto brokerDto = addBrokerNamedBroker();

        UpdateBrokerTransferFeeCommand updateCommand = new UpdateBrokerTransferFeeCommand();
        updateCommand.setMaximumFee(new BigDecimal("210"));
        updateCommand.setFeeCurrency("USD");

        BrokerEntityNotFoundException ex = assertThrows(BrokerEntityNotFoundException.class,
                () -> brokerFeeService.updateTransferFee(brokerDto.getId(), 1L, updateCommand));
        assertThat(ex.getMessage(), matchesPattern("No BrokerTransferFee with id: [0-9]+ linked to Broker with id: [0-9]+"));
    }

    @Test
    void updatingTransferFeesWithAnotherBrokersIdShouldFail() {
        BrokerDto brokerDto = addBrokerNamedBroker();

        CreateBrokerTransferFeeCommand createCommand = new CreateBrokerTransferFeeCommand();
        createCommand.setFromDate(LocalDate.of(2000, 1, 1));
        BrokerTransferFeeDto feeDto = brokerFeeService.addTransferFee(brokerDto.getId(), createCommand);

        CreateBrokerCommand createBrokerCommand = new CreateBrokerCommand();
        createBrokerCommand.setName("Another Broker");
        BrokerDto another = brokerService.addBroker(createBrokerCommand);

        UpdateBrokerTransferFeeCommand updateCommand = new UpdateBrokerTransferFeeCommand();
        updateCommand.setMaximumFee(new BigDecimal("210"));
        updateCommand.setFeeCurrency("USD");

        BrokerEntityNotFoundException ex = assertThrows(BrokerEntityNotFoundException.class,
                () -> brokerFeeService.updateTransferFee(another.getId(), feeDto.getId(), updateCommand));
        assertThat(ex.getMessage(), matchesPattern("No BrokerTransferFee with id: [0-9]+ linked to Broker with id: [0-9]+"));
    }

    @Test
    void updatingTransferFeeToOverlapShouldFail() {
        BrokerDto brokerDto = addBrokerNamedBroker();

        CreateBrokerTransferFeeCommand createCommand = new CreateBrokerTransferFeeCommand();
        createCommand.setFromDate(LocalDate.of(2000, 1, 1));
        createCommand.setToDate(LocalDate.of(2001, 1, 1));
        brokerFeeService.addTransferFee(brokerDto.getId(), createCommand);

        createCommand.setFromDate(LocalDate.of(2002, 1, 1));
        createCommand.setToDate(LocalDate.of(2003, 1, 1));
        BrokerTransferFeeDto feeDto = brokerFeeService.addTransferFee(brokerDto.getId(), createCommand);

        UpdateBrokerTransferFeeCommand updateCommand = new UpdateBrokerTransferFeeCommand();
        updateCommand.setFromDate(LocalDate.of(2000,6,1));

        UniqueViolationException ex = assertThrows(UniqueViolationException.class,
                () -> brokerFeeService.updateTransferFee(brokerDto.getId(), feeDto.getId(), updateCommand));
        assertThat(ex.getMessage(), startsWith("BrokerTransferFee: You provided the range 2000-06-01 -  but these periods overlap with it: [2000-01-01--2001-01-01 (id: "));
    }

    @Test
    void gettingABrokerTransferFee() {
        BrokerDto brokerDto = addBrokerNamedBroker();
        CreateBrokerTransferFeeCommand createCommand = new CreateBrokerTransferFeeCommand();
        createCommand.setFromDate(LocalDate.of(2000, 1, 1));
        createCommand.setToDate(LocalDate.of(2001, 1, 1));
        brokerFeeService.addTransferFee(brokerDto.getId(), createCommand);

        BrokerTransferFeeDto feeDto = brokerFeeService.getTransferFee(brokerDto.getId(),
                LocalDate.of(2000,6,1), null);
        assertEquals(LocalDate.of(2000,1,1), feeDto.getFromDate());
    }

    @Test
    void gettingABrokerTransferFeeOutsideValidRange() {
        BrokerDto brokerDto = addBrokerNamedBroker();
        CreateBrokerTransferFeeCommand createCommand = new CreateBrokerTransferFeeCommand();
        createCommand.setToDate(LocalDate.of(2001, 1, 1));
        brokerFeeService.addTransferFee(brokerDto.getId(), createCommand);

        createCommand.setFromDate(LocalDate.of(2002, 1, 1));
        createCommand.setToDate(null);
        createCommand.setTransferredCurrency("USD");
        brokerFeeService.addTransferFee(brokerDto.getId(), createCommand);

        BrokerEntityNotFoundException ex = assertThrows(BrokerEntityNotFoundException.class,
                () -> brokerFeeService.getTransferFee(brokerDto.getId(),
                        LocalDate.of(2001,6,1), "GBP"));
        assertThat(ex.getMessage(), matchesPattern("^No transfer fee was found for broker [0-9]+, date 2001-06-01 and currency GBP"));

    }

    @Test
    void gettingTransferFeeShouldNotSucceedForAnotherCurrency() {
        BrokerDto brokerDto = addBrokerNamedBroker();
        CreateBrokerTransferFeeCommand createCommand = new CreateBrokerTransferFeeCommand();
        createCommand.setToDate(LocalDate.of(2001, 1, 1));
        brokerFeeService.addTransferFee(brokerDto.getId(), createCommand);

        createCommand.setFromDate(LocalDate.of(2002, 1, 1));
        createCommand.setToDate(null);
        createCommand.setTransferredCurrency("EUR");
        brokerFeeService.addTransferFee(brokerDto.getId(), createCommand);

        BrokerEntityNotFoundException ex = assertThrows(BrokerEntityNotFoundException.class,
                () -> brokerFeeService.getTransferFee(brokerDto.getId(),
                        LocalDate.of(2004,6,1), "USD"));
        assertThat(ex.getMessage(), matchesPattern("^No transfer fee was found for broker [0-9]+, date 2004-06-01 and currency USD"));
    }







    @Test
    void addValidBrokerGlobalFee() {
        BrokerDto brokerDto = addBrokerNamedBroker();

        CreateBrokerGlobalFeeCommand command = new CreateBrokerGlobalFeeCommand();
        command.setBalanceFeeGlobalLimitCurrency("EUR");
        command.setFromDate(LocalDate.of(2000, 1, 1));
        command.setToDate(LocalDate.of(2004, 1, 1));

        BrokerGlobalFeeDto dto = brokerFeeService.addGlobalFee(brokerDto.getId(), command);
        assertEquals("EUR", dto.getBalanceFeeGlobalLimitCurrency());
    }

    @Test
    void addingGlobalFeeDetailsForOverlappingPeriodAllDatesDefined() {
        BrokerDto brokerDto = addBrokerNamedBroker();

        CreateBrokerGlobalFeeCommand command = new CreateBrokerGlobalFeeCommand();
        command.setGlobalFixedFeeCurrency("EUR");
        command.setFromDate(LocalDate.of(2000, 1, 1));
        command.setToDate(LocalDate.of(2004, 1, 1));
        Long id = brokerDto.getId();
        brokerFeeService.addGlobalFee(id, command);

        // Overlap with previous
        command.setFromDate(LocalDate.of(2001, 1, 1));
        command.setToDate(LocalDate.of(2001, 1, 5));

        UniqueViolationException ex = assertThrows(UniqueViolationException.class, () -> brokerFeeService.addGlobalFee(id, command));
        assertThat(ex.getMessage(), startsWith("BrokerGlobalFee: You provided the range 2001-01-01 - 2001-01-05 but these periods overlap with it: [2000-01-01--2004-01-01 (id: "));
    }

    @Test
    void addingGlobalFeeDetailsForOverLappingPeriodExistingHaveUndefined() {
        BrokerDto brokerDto = addBrokerNamedBroker();

        CreateBrokerGlobalFeeCommand command = new CreateBrokerGlobalFeeCommand();
        command.setGlobalFixedFeeCurrency("EUR");
        command.setFromDate(LocalDate.of(2000, 1, 1));
        Long id = brokerDto.getId();
        brokerFeeService.addGlobalFee(id, command); // From 2000-01-01 indefinitely

        command.setFromDate(null);
        command.setToDate(LocalDate.of(1999, 10, 1)); // All the way until 1999-10-01
        brokerFeeService.addGlobalFee(id, command);

        command.setFromDate(LocalDate.of(1998, 1, 1));
        command.setToDate(LocalDate.of(1999, 1, 1));
        UniqueViolationException ex = assertThrows(UniqueViolationException.class, () -> brokerFeeService.addGlobalFee(id, command));
        assertThat(ex.getMessage(), startsWith("BrokerGlobalFee: You provided the range 1998-01-01 - 1999-01-01 but these periods overlap with it: [--1999-10-01 (id: "));

        command.setFromDate(LocalDate.of(2002, 1, 1));
        command.setToDate(LocalDate.of(2008, 1, 1));
        ex = assertThrows(UniqueViolationException.class, () -> brokerFeeService.addGlobalFee(id, command));
        assertThat(ex.getMessage(), startsWith("BrokerGlobalFee: You provided the range 2002-01-01 - 2008-01-01 but these periods overlap with it: [2000-01-01-- (id: "));

    }

    @Test
    void addingGlobalFeeDetailsForOverlappingPeriodNewHaveUndefined() {
        BrokerDto brokerDto = addBrokerNamedBroker();

        CreateBrokerGlobalFeeCommand command = new CreateBrokerGlobalFeeCommand();
        command.setGlobalFixedFeeCurrency("EUR");
        command.setFromDate(LocalDate.of(2000, 1, 1));
        command.setToDate(LocalDate.of(2002, 1, 1));
        Long id = brokerDto.getId();
        brokerFeeService.addGlobalFee(id, command);

        command.setFromDate(null);
        command.setToDate(LocalDate.of(2001, 1, 1));
        UniqueViolationException ex = assertThrows(UniqueViolationException.class, () -> brokerFeeService.addGlobalFee(id, command));
        assertThat(ex.getMessage(), startsWith("BrokerGlobalFee: You provided the range  - 2001-01-01 but these periods overlap with it: [2000-01-01--2002-01-01 (id: "));


        command.setFromDate(LocalDate.of(2001, 1, 1));
        ex = assertThrows(UniqueViolationException.class, () -> brokerFeeService.addGlobalFee(id, command));
        assertThat(ex.getMessage(), startsWith("BrokerGlobalFee: You provided the range 2001-01-01 - 2001-01-01 but these periods overlap with it: [2000-01-01--2002-01-01 (id: "));

    }



    @Test
    void addingGlobalFeeDetailsForNotExistingBroker() {
        CreateBrokerGlobalFeeCommand command = new CreateBrokerGlobalFeeCommand();
        command.setGlobalFixedFeeCurrency("EUR");

        BrokerEntityNotFoundException ex = assertThrows(BrokerEntityNotFoundException.class,
                () -> brokerFeeService.addGlobalFee(1L, command));
        assertEquals("Broker not found with id: 1", ex.getMessage());
    }

    @Test
    void updatingGlobalFeeDetailsWithValidDetails() {
        BrokerDto brokerDto = addBrokerNamedBroker();

        CreateBrokerGlobalFeeCommand createCommand = new CreateBrokerGlobalFeeCommand();
        createCommand.setFromDate(LocalDate.of(2000, 1, 1));
        createCommand.setToDate(LocalDate.of(2001, 1, 1));

        BrokerGlobalFeeDto feeDto = brokerFeeService.addGlobalFee(brokerDto.getId(), createCommand);

        UpdateBrokerGlobalFeeCommand updateCommand = new UpdateBrokerGlobalFeeCommand();
        updateCommand.setGlobalFixedFeeAmt(new BigDecimal("210"));
        updateCommand.setGlobalFixedFeeCurrency("USD");
        updateCommand.setFromDate(LocalDate.of(2000,6,1));
        brokerFeeService.updateGlobalFee(brokerDto.getId(), feeDto.getId(), updateCommand);

        BrokerGlobalFeeDto queried = brokerFeeService.getGlobalFeeById(feeDto.getId());
        assertEquals(LocalDate.of(2000, 6, 1), queried.getFromDate());
        assertEquals(new BigDecimal("210"), queried.getGlobalFixedFeeAmt());
        assertEquals("USD", queried.getGlobalFixedFeeCurrency());
    }

    @Test
    void updatingGlobalFeeDetailsForInvalidId() {
        BrokerDto brokerDto = addBrokerNamedBroker();

        UpdateBrokerGlobalFeeCommand updateCommand = new UpdateBrokerGlobalFeeCommand();
        updateCommand.setGlobalFixedFeeAmt(new BigDecimal("210"));
        updateCommand.setBalanceFeeGlobalLimitCurrency("USD");

        BrokerEntityNotFoundException ex = assertThrows(BrokerEntityNotFoundException.class,
                () -> brokerFeeService.updateGlobalFee(brokerDto.getId(), 1L, updateCommand));
        assertThat(ex.getMessage(), matchesPattern("No BrokerGlobalFee with id: [0-9]+ linked to Broker with id: [0-9]+"));
    }

    @Test
    void updatingGlobalFeesWithAnotherBrokersIdShouldFail() {
        BrokerDto brokerDto = addBrokerNamedBroker();

        CreateBrokerGlobalFeeCommand createCommand = new CreateBrokerGlobalFeeCommand();
        createCommand.setFromDate(LocalDate.of(2000, 1, 1));
        BrokerGlobalFeeDto feeDto = brokerFeeService.addGlobalFee(brokerDto.getId(), createCommand);

        CreateBrokerCommand createBrokerCommand = new CreateBrokerCommand();
        createBrokerCommand.setName("Another Broker");
        BrokerDto another = brokerService.addBroker(createBrokerCommand);

        UpdateBrokerGlobalFeeCommand updateCommand = new UpdateBrokerGlobalFeeCommand();
        updateCommand.setBalanceFeeGlobalLimit(new BigDecimal("210"));
        updateCommand.setBalanceFeeGlobalLimitCurrency("USD");

        BrokerEntityNotFoundException ex = assertThrows(BrokerEntityNotFoundException.class,
                () -> brokerFeeService.updateGlobalFee(another.getId(), feeDto.getId(), updateCommand));
        assertThat(ex.getMessage(), matchesPattern("No BrokerGlobalFee with id: [0-9]+ linked to Broker with id: [0-9]+"));
    }

    @Test
    void updatingGlobalFeeToOverlapShouldFail() {
        BrokerDto brokerDto = addBrokerNamedBroker();

        CreateBrokerGlobalFeeCommand createCommand = new CreateBrokerGlobalFeeCommand();
        createCommand.setFromDate(LocalDate.of(2000, 1, 1));
        createCommand.setToDate(LocalDate.of(2001, 1, 1));
        brokerFeeService.addGlobalFee(brokerDto.getId(), createCommand);

        createCommand.setFromDate(LocalDate.of(2002, 1, 1));
        createCommand.setToDate(LocalDate.of(2003, 1, 1));
        BrokerGlobalFeeDto feeDto = brokerFeeService.addGlobalFee(brokerDto.getId(), createCommand);

        UpdateBrokerGlobalFeeCommand updateCommand = new UpdateBrokerGlobalFeeCommand();
        updateCommand.setFromDate(LocalDate.of(2000,6,1));

        UniqueViolationException ex = assertThrows(UniqueViolationException.class,
                () -> brokerFeeService.updateGlobalFee(brokerDto.getId(), feeDto.getId(), updateCommand));
        assertThat(ex.getMessage(), startsWith("BrokerGlobalFee: You provided the range 2000-06-01 -  but these periods overlap with it: [2000-01-01--2001-01-01 (id: "));
    }


    BrokerDto addBrokerNamedBroker() {
        CreateBrokerCommand createBrokerCommand = new CreateBrokerCommand();
        createBrokerCommand.setName("Broker");
        return brokerService.addBroker(createBrokerCommand);
    }

}