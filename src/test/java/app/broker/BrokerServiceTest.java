package app.broker;

import app.broker.fees.transfer.BrokerTransferFeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrokerServiceTest {

    @Mock
    BrokerRepository brokerRepository;

    @Mock
    BrokerTransferFeeRepository brokerTransferFeeRepository;

    @Spy
    BrokerMapperImpl brokerMapper = new BrokerMapperImpl();

    @InjectMocks
    BrokerService brokerService;


    @Test
    void addingValidBroker() {
        CreateBrokerCommand command = new CreateBrokerCommand();
        command.setName("New broker");
        BrokerDto dto = brokerService.addBroker(command);

        verify(brokerRepository).save(new Broker("New broker"));
        assertEquals("New broker", dto.getName());
    }

    @Test
    void addingDuplicateNameThrowsException() {
        when(brokerRepository.countByName("broker")).thenReturn(1);

        CreateBrokerCommand command = new CreateBrokerCommand("broker");

        UniqueViolationException ex = assertThrows(UniqueViolationException.class,
                () -> brokerService.addBroker(command));
        assertEquals("Broker names must be unique. A broker already exists with the name: broker", ex.getMessage());
    }

    @Test
    void updatingToUniqueNameShouldSucceed() {
        when(brokerRepository.findById(3L)).thenReturn(Optional.of(new Broker("old name")));
        when(brokerRepository.countByName("new name")).thenReturn(0);

        UpdateBrokerCommand command = new UpdateBrokerCommand("new name");
        BrokerDto broker = brokerService.updateBroker(3L, command);

        assertEquals("new name", broker.getName());
    }

    @Test
    void updatingMissingBrokerShouldFail() {
        when(brokerRepository.findById(3L)).thenReturn(Optional.empty());

        UpdateBrokerCommand command = new UpdateBrokerCommand("new name");

        BrokerEntityNotFoundException ex = assertThrows(BrokerEntityNotFoundException.class,
                () -> brokerService.updateBroker(3L, command));
        assertEquals("Broker not found with id: 3", ex.getMessage());
    }

    @Test
    void updatingToPresentNameShouldFail() {
        when(brokerRepository.findById(3L)).thenReturn(Optional.of(new Broker("old name")));
        when(brokerRepository.countByName("new name")).thenReturn(1);

        UpdateBrokerCommand command = new UpdateBrokerCommand("new name");

        UniqueViolationException ex = assertThrows(UniqueViolationException.class,
                () -> brokerService.updateBroker(3L, command));
        assertEquals("Cannot update broker. Another broker already exists with the name: new name", ex.getMessage());
    }

    @Test
    void updatingWithoutNameChangeShouldSucceed() {
        when(brokerRepository.findById(3L)).thenReturn(Optional.of(new Broker("old name")));

        UpdateBrokerCommand command = new UpdateBrokerCommand("old name");
        BrokerDto broker = brokerService.updateBroker(3L, command);

        assertEquals("old name", broker.getName());
    }


}