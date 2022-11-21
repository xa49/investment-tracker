package app.broker.fees;

import app.broker.BrokerEntityNotFoundException;
import app.broker.fees.global.*;
import app.broker.fees.transfer.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
@Slf4j
public class BrokerFeeService {

    private final BrokerTransferFeeAdderTemplate transferFeeAdder;
    private final BrokerGlobalFeeAdderTemplate globalFeeAdder;

    private final BrokerTransferFeeRepository transferFeeRepository;
    private final BrokerGlobalFeeRepository globalFeeRepository;
    private final BrokerGlobalFeeMapper globalFeeMapper;
    private final BrokerTransferFeeMapper transferFeeMapper;


    public List<BrokerTransferFeeDto> listTransferFeesAtBroker(Long brokerId) {
        return transferFeeMapper.toDto(transferFeeRepository.findAllByBrokerId(brokerId));
    }

    public BrokerTransferFeeDto addTransferFee(Long brokerId, CreateBrokerTransferFeeCommand command) {
        return transferFeeMapper.toDto((BrokerTransferFee) transferFeeAdder.addItem(brokerId, command.getFromDate(),
                command.getToDate(), command));
    }

    @Transactional
    public void updateTransferFee(Long brokerId, Long transferFeeId, UpdateBrokerTransferFeeCommand command) {
        transferFeeAdder.updateItem(brokerId, transferFeeId, command.getFromDate(), command.getToDate(), command);
    }

    public BrokerTransferFeeDto getTransferFeeById(Long id) {
        Optional<BrokerTransferFee> fee = transferFeeRepository.findById(id);
        if (fee.isPresent()) {
            return transferFeeMapper.toDto(fee.get());
        } else {
            throw new BrokerEntityNotFoundException("BrokerTransferFee not found with id: " + id);
        }
    }

    public BrokerTransferFeeDto getTransferFee(Long brokerId, LocalDate date, String currency) {
        Optional<BrokerTransferFee> fee =
                transferFeeRepository.getTransferFeeForBrokerAndDayAndCurrency(brokerId, date, currency);
        if (fee.isPresent()) {
            return transferFeeMapper.toDto(fee.get());
        } else {
            throw new BrokerEntityNotFoundException("No transfer fee was found for broker " + brokerId
                    + ", date " + date + " and currency " + currency);
        }
    }


    public List<BrokerGlobalFeeDto> listGlobalFeesAtBroker(Long brokerId) {
        return globalFeeMapper.toDto(globalFeeRepository.findAllByBrokerId(brokerId));
    }

    public BrokerGlobalFeeDto addGlobalFee(Long brokerId, CreateBrokerGlobalFeeCommand command) {
        return globalFeeMapper.toDto((BrokerGlobalFee) globalFeeAdder.addItem(brokerId, command.getFromDate(),
                command.getToDate(), command));
    }

    @Transactional
    public void updateGlobalFee(Long brokerId, Long feeId, UpdateBrokerGlobalFeeCommand command) {
        globalFeeAdder.updateItem(brokerId, feeId, command.getFromDate(), command.getToDate(), command);
    }

    public BrokerGlobalFeeDto getGlobalFeeById(Long feeId) {
        Optional<BrokerGlobalFee> fee = globalFeeRepository.findById(feeId);
        if (fee.isPresent()) {
            return globalFeeMapper.toDto(fee.get());
        } else {
            throw new BrokerEntityNotFoundException("BrokerGlobalFee not found with id: " + feeId);
        }
    }
}
