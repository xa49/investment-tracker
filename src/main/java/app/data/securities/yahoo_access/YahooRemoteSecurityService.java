package app.data.securities.yahoo_access;

import app.data.securities.price.SecurityPrice;
import app.data.securities.price.SecurityPriceOfDay;
import app.data.securities.security.Security;
import app.util.MissingDataException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class YahooRemoteSecurityService implements RemoteSecurityService {

    public Map<LocalDate, SecurityPrice> getPrices(String ticker, LocalDate from, LocalDate to) {
        GregorianCalendar calendarFrom = GregorianCalendar.from(from.atStartOfDay(ZoneId.of("Europe/Paris")));
        GregorianCalendar calendarToExclusive = GregorianCalendar.from(
                to.plusDays(1).atStartOfDay(ZoneId.of("Europe/Paris")));
        try {
            Stock stock = YahooFinance.get(ticker, calendarFrom, calendarToExclusive, Interval.DAILY);
            return createMapFromHistoricalPrices(stock);
        } catch (IOException | NullPointerException e) {
            throw new MissingDataException("Could not get stock prices from Yahoo for " + ticker
                    + " from " + from + " to " + to, e);
        }
    }

    private Map<LocalDate, SecurityPrice> createMapFromHistoricalPrices(Stock stock) throws IOException {
        return stock.getHistory().stream()
                .map(quote -> mapHistoricalQuoteToPrice(quote, stock))
                .collect(Collectors.toMap(
                        SecurityPrice::getDate,
                        p -> p
                ));
    }

    @Override
    public Optional<Security> getSecurityDetails(String ticker) {
        try {
            Stock stock = YahooFinance.get(ticker);
            Security security =
                    new Security(stock.getSymbol(), stock.getName(), stock.getStockExchange(), stock.getCurrency());
            return Optional.of(security);

        } catch (IOException | NullPointerException e) {
            log.debug("Could not get security details for ticker {}", ticker);
            return Optional.empty();
        }
    }

    private SecurityPriceOfDay mapHistoricalQuoteToPrice(HistoricalQuote quote, Stock stock) {
        return SecurityPriceOfDay.builder()
                .ticker(quote.getSymbol())
                .date(LocalDateTime.ofInstant(
                        quote.getDate().toInstant(),
                        quote.getDate().getTimeZone().toZoneId()).toLocalDate())
                .currency(stock.getCurrency())
                .open(quote.getOpen())
                .close(quote.getClose())
                .adjClose(quote.getAdjClose())
                .low(quote.getLow())
                .high(quote.getHigh())
                .volume(quote.getVolume())
                .build();
    }
}
