package app.broker.fees.calculator;

import app.analysis.CashValue;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@AllArgsConstructor
public class FeeCalculatorService {

    @PersistenceContext
    EntityManager entityManager;

    private final TransferFeeCalculator transferFeeCalculator;
    private final TransactionCommissionCalculator commissionCalculator;

    /**
     * Calculates the fee that will be charged by the broker for the transfer of the specified amount at the transfer date.
     *
     * @param accountId         The id of the account making the transfer.
     * @param date              The date of transfer.
     * @param transferredAmount The amount to be transferred in the specified currency.
     * @param currency          The currency of the transfer.
     * @return The Fee (as a negative amount) that will be charged by the broker, specified in the currency charged by the broker.
     */
    public CashValue getTransferFee(Long accountId, LocalDate date, BigDecimal transferredAmount, String currency) {
        return transferFeeCalculator.getTransferFee(accountId, date, transferredAmount, currency);
    }

    /**
     * Calculates the fee charged by the broker to either open or close the specified position.
     *
     * @param accountId     The id of the account which handles the transaction.
     * @param date          The date of the transaction.
     * @param market        The market on which the transaction is made.
     * @param value         The value of the transaction, e.g. number of securities × price
     * @param valueCurrency The currency in which `value` is calculated.
     * @return The Fee (as a negative amount) charged by the broker for the transaction, specified in the currency charged by the broker.
     */
    public CashValue getCommissionOnTransaction(
            Long accountId, LocalDate date, String market, BigDecimal value, String valueCurrency) {
        return commissionCalculator.getCommissionOnTransaction(accountId, date, market, value, valueCurrency);
    }
}
