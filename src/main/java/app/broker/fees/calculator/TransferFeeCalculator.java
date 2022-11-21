package app.broker.fees.calculator;

import app.broker.Broker;
import app.broker.account.BrokerAccountService;
import app.broker.fees.BrokerFeeService;
import app.broker.fees.transfer.BrokerTransferFeeDto;
import app.data.DataService;
import app.analysis.CashValue;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@AllArgsConstructor
@Slf4j
public class TransferFeeCalculator {
    private final BrokerAccountService accountService;
    private final BrokerFeeService feeService;
    private final DataService dataService;

    private final PercentageBasedLimitedFeeCalculator limitedFeeCalculator;

    /**
     * Calculates the fee that will be charged by the broker for the transfer of the specified amount at the transfer date.
     *
     * @param accountId           The id of the account making the transfer.
     * @param date                The date of transfer.
     * @param transferredAmount   The amount to be transferred in the specified transferredCurrency.
     * @param transferredCurrency The transferredCurrency of the transfer.
     * @return The Fee (as a negative amount) that will be charged by the broker, specified in the transferredCurrency charged by the broker.
     */
    public CashValue getTransferFee(
            Long accountId, LocalDate date, BigDecimal transferredAmount, String transferredCurrency) {
        Long brokerId = getBroker(accountId, date).getId();

        BrokerTransferFeeDto transferFeeDto = feeService.getTransferFee(brokerId, date, transferredCurrency);
        String feeCurrency = transferFeeDto.getFeeCurrency();
        BigDecimal rate = dataService.getExchangeRate(transferredCurrency, feeCurrency, date);

        BigDecimal feeAbsoluteValue = limitedFeeCalculator.calculateFee(transferredAmount.multiply(rate),
                transferFeeDto.getPercentFee(), transferFeeDto.getMinimumFee(), transferFeeDto.getMaximumFee());
        log.debug("Transfer fee for [accountId: {}, {}, {}, {}] is {} {}", accountId, date, transferredAmount,
                transferredCurrency, feeCurrency, feeAbsoluteValue);
        return new CashValue(feeAbsoluteValue.negate(), feeCurrency);
    }

    private Broker getBroker(Long accountId, LocalDate date) {
        return accountService.getProductAssociation(accountId, date)
                .getProduct().getBroker();
    }
}
