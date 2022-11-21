package app.broker;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Sql(scripts = "classpath:/cleanbroker.sql")
class BrokerServiceIT {

    @Autowired
    BrokerService brokerService;

    @Test
    void addBroker() {
        CreateBrokerCommand command = new CreateBrokerCommand();
        command.setName("New broker");

        BrokerDto dto = brokerService.addBroker(command);
        assertEquals("New broker", dto.getName());
        assertNotNull(dto.getId());
    }

    @Test
    void addingBrokerWithDuplicateNameShouldFail() {
        CreateBrokerCommand command = new CreateBrokerCommand();
        command.setName("New broker");
        brokerService.addBroker(command);

        UniqueViolationException ex = assertThrows(UniqueViolationException.class, () -> brokerService.addBroker(command));
        assertEquals("Broker names must be unique. A broker already exists with the name: New broker", ex.getMessage());
    }

    @Test
    void updatingBrokerNameValid() {
        CreateBrokerCommand command = new CreateBrokerCommand();
        command.setName("New broker");
        Long id = brokerService.addBroker(command).getId();

        UpdateBrokerCommand updateBrokerCommand = new UpdateBrokerCommand();
        updateBrokerCommand.setName("Renamed");
        BrokerDto updated = brokerService.updateBroker(id, updateBrokerCommand);
        assertEquals("Renamed", updated.getName());

        BrokerDto queried = brokerService.getBrokerById(id);
        assertEquals("Renamed", queried.getName());
    }

    @Test
    void updatingBrokerNotExisting() {
        // Empty broker table
        UpdateBrokerCommand updateBrokerCommand = new UpdateBrokerCommand();
        updateBrokerCommand.setName("Renamed");
        BrokerEntityNotFoundException ex = assertThrows(BrokerEntityNotFoundException.class,
                () -> brokerService.updateBroker(1L, updateBrokerCommand));
        assertEquals("Broker not found with id: 1", ex.getMessage());
    }

    @Test
    void listBrokers() {
        CreateBrokerCommand command = new CreateBrokerCommand();
        command.setName("Adding first");
        brokerService.addBroker(command);
        command.setName("Adding second");
        brokerService.addBroker(command);

        List<BrokerDto> brokers = brokerService.listBrokers();
        assertEquals(2, brokers.size());
        assertEquals("Adding first", brokers.get(0).getName()); // Alphabetic sorting
        assertEquals("Adding second", brokers.get(1).getName());
    }

    @Test
    void getBrokerDetailsByIdFound() {
        CreateBrokerCommand command = new CreateBrokerCommand();
        command.setName("New broker");

        BrokerDto dto = brokerService.addBroker(command);
        BrokerDto queried = brokerService.getBrokerById(dto.getId());
        assertEquals("New broker", queried.getName());
    }

    @Test
    void getBrokerDetailsByIdNotFound() {
        BrokerEntityNotFoundException ex = assertThrows(BrokerEntityNotFoundException.class, () -> brokerService.getBrokerById(1L));
        assertEquals("Broker not found with id: 1", ex.getMessage());
    }


}