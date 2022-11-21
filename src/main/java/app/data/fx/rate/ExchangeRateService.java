package app.data.fx.rate;

import java.time.LocalDate;
import java.util.Optional;

public interface ExchangeRateService {
    /**
     * @param sourceCurrency      The three-letter ISO-4217 code of the currency being converted from.
     * @param destinationCurrency The three-letter ISO-4217 code of the currency being converted to.
     * @return The one-unit-for-one-unit exchange rate so that <i>amount in sourceCurrency</i> × <u>return value</u> =
     * <i>amount in destinationCurrency</i> <b>exactly on the specified date</b>. It is most likely that such query will
     * fail for weekends and holidays.
     */
    Optional<Rate> getExchangeRateDetails(String sourceCurrency, String destinationCurrency, LocalDate targetDate);

    /**
     * This version of the method allows for backward-looking to find a matching rate when no rate found for the exact
     * target date.
     *
     * @param sourceCurrency             The three-letter ISO-4217 code of the currency being converted from.
     * @param destinationCurrency        The three-letter ISO-4217 code of the currency being converted to.
     * @param backwardDayOffsetTolerance The number of days that the exchange rate can be off from the target date.
     * @return The one-unit-for-one-unit exchange rate so that <i>amount in sourceCurrency</i> × <u>return value</u> =
     * <i>amount in destinationCurrency</i>.
     */
    Optional<Rate> getExchangeRateDetails(String sourceCurrency, String destinationCurrency, LocalDate targetDate,
                                int backwardDayOffsetTolerance);
}
