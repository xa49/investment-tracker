package app.broker.fees.calculator;

import app.broker.account.BrokerAccountService;
import app.broker.account.association.ProductAssociation;
import app.broker.product.BrokerProductService;
import app.broker.product.commission.ProductCommissionDto;
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
public class TransactionCommissionCalculator {

    private final BrokerAccountService accountService;
    private final BrokerProductService productService;
    private final DataService dataService;
    private final PercentageBasedLimitedFeeCalculator limitedFeeCalculator;

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
    public CashValue getCommissionOnTransaction(Long accountId, LocalDate date, String market, BigDecimal value,
                                                String valueCurrency) {
        ProductAssociation service = accountService.getProductAssociation(accountId, date);
        ProductCommissionDto commissionDto = productService.getCommission(service.getProduct().getId(), market, date);
        String commissionCurrency = commissionDto.getCurrency();
        BigDecimal valueInCommissionCurrency = value
                .multiply(dataService.getExchangeRate(valueCurrency, commissionCurrency, date));

        BigDecimal feeAbsoluteValue = limitedFeeCalculator
                .calculateFee(valueInCommissionCurrency, commissionDto.getPercentFee(),
                commissionDto.getMinimumFee(), commissionDto.getMaximumFee());
        return new CashValue(feeAbsoluteValue.negate(), commissionCurrency); // could catch to add more info

    }
}
