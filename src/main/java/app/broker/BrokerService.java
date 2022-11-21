package app.broker;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
@Slf4j
public class BrokerService {

    private final BrokerRepository brokerRepository;
    private final BrokerMapper brokerMapper;

    public List<BrokerDto> listBrokers() {
        return brokerMapper.toDto(brokerRepository.findAll());
    }

    public BrokerDto getBrokerById(Long id) {
        Optional<Broker> broker = brokerRepository.findById(id);
        if (broker.isPresent()) {
            return brokerMapper.toDto(broker.get());
        } else {
            throw new BrokerEntityNotFoundException("Broker not found with id: " + id);
        }
    }

    public BrokerDto addBroker(CreateBrokerCommand command) {
        if (brokerRepository.countByName(command.getName()) > 0) {
            throw new UniqueViolationException("Broker names must be unique. A broker already exists with the name: " +
                    command.getName());
        }

        Broker broker = new Broker(command.getName());
        brokerRepository.save(broker);
        log.debug("Broker added to database: {}. Command was: {}", broker, command);
        return brokerMapper.toDto(broker);
    }

    @Transactional
    public BrokerDto updateBroker(Long id, UpdateBrokerCommand command) {
        Optional<Broker> broker = brokerRepository.findById(id);
        if (broker.isEmpty()) {
            throw new BrokerEntityNotFoundException("Broker not found with id: " + id);
        }

        Broker brokerToUpdate = broker.get();
        if (!command.getName().equals(brokerToUpdate.getName())) {
            if (brokerRepository.countByName(command.getName()) == 0) {
                brokerToUpdate.setName(command.getName());
            } else {
                throw new UniqueViolationException("Cannot update broker. Another broker already exists with the name: "
                        + command.getName());
            }
        }
        return brokerMapper.toDto(brokerToUpdate);
    }

}
