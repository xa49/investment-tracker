package app.data.fx.mnb_access;

import hu.mnb.webservices.*;
import org.springframework.stereotype.Service;
import org.tempuri.MNBArfolyamServiceSoapImpl;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MNBQueryService {
    private final MNBArfolyamServiceSoapImpl impl = new MNBArfolyamServiceSoapImpl();
    private final MNBArfolyamServiceSoap service = impl.getCustomBindingMNBArfolyamServiceSoap();
    private final ObjectFactory factory = new ObjectFactory();

    /**
     * Uses MNB's SOAP service to query 30+ exchange rates against HUF.
     *
     * @return All available one-unit-for-one-unit exchange rates within the specified period, i.e. the amount of HUF
     * that one unit of source currency is worth.
     */
    public Map<LocalDate, BigDecimal> getHufRates(String sourceAbbreviation, LocalDate from, LocalDate to) {
        String response = queryHufRates(sourceAbbreviation, from, to);
        return parseRateResponse(response);
    }

    public Optional<MNBCurrency> getCurrencyDetails(String currencyAbbreviation) {
        String response = queryCurrencyDetails(currencyAbbreviation);
        MNBCurrency mnbCurrency = parseCurrencyResponse(response);
        if (mnbCurrency.isPresent()) {
            return Optional.of(mnbCurrency);
        } else {
            return Optional.empty();
        }
    }

    private String queryHufRates(String currencyAbbreviation, LocalDate from, LocalDate to) {
        GetExchangeRatesRequestBody requestBody = factory.createGetExchangeRatesRequestBody();
        requestBody.setStartDate(
                factory.createGetExchangeRatesRequestBodyStartDate(from.toString())
        );
        requestBody.setEndDate(
                factory.createGetExchangeRatesRequestBodyEndDate(to.toString())
        );
        requestBody.setCurrencyNames(
                factory.createGetCurrencyUnitsRequestBodyCurrencyNames(currencyAbbreviation)
        );

        try {
            return service.getExchangeRates(requestBody).getGetExchangeRatesResult().getValue();
        } catch (MNBArfolyamServiceSoapGetExchangeRatesStringFaultFaultMessage e) {
            throw new IllegalStateException("Could not get exchange rates from MNB: " + currencyAbbreviation + " "
                    + from + "--" + to, e);
        }
    }

    private Map<LocalDate, BigDecimal> parseRateResponse(String xml) {
        try {
            JAXBContext ctx = JAXBContext.newInstance(MNBSingleCurrencyRates.class);
            Unmarshaller unmarshaller = ctx.createUnmarshaller();
            MNBSingleCurrencyRates response = (MNBSingleCurrencyRates) unmarshaller.unmarshal(new StringReader(xml));
            return dateMappedRates(response);
        } catch (Exception e) {
            System.out.println(xml);
            throw new IllegalStateException("Error parsing xml returned by MNB exchange rate query.", e);
        }
    }

    private Map<LocalDate, BigDecimal> dateMappedRates(MNBSingleCurrencyRates rates) {
        if (rates.isPresent()) {
            return rates.getRates().stream()
                    .collect(Collectors.toMap(
                            r -> LocalDate.parse(r.getDate()),
                            r -> new BigDecimal(r.getPresentationRate().replace(",", "."))
                                    .divide(new BigDecimal(r.getPresentationUnits()), MathContext.DECIMAL64)
                    ));
        } else {
            return new HashMap<>();
        }
    }


    private String queryCurrencyDetails(String currencyAbbreviation) {
        // Response format: <MNBCurrencyUnits><Units><Unit curr="JPY">100</Unit></Units></MNBCurrencyUnits>
        GetCurrencyUnitsRequestBody requestBody = factory.createGetCurrencyUnitsRequestBody();
        requestBody.setCurrencyNames(
                factory.createGetCurrencyUnitsRequestBodyCurrencyNames(currencyAbbreviation)
        );
        try {
            return service.getCurrencyUnits(requestBody).getGetCurrencyUnitsResult().getValue();
        } catch (MNBArfolyamServiceSoapGetCurrencyUnitsStringFaultFaultMessage e) {
            throw new IllegalStateException("Could not get currency units from MNB.", e);
        }
    }

    private MNBCurrency parseCurrencyResponse(String xml) {
        try {
            JAXBContext ctx = JAXBContext.newInstance(MNBCurrency.class);
            Unmarshaller unmarshaller = ctx.createUnmarshaller();
            return (MNBCurrency) unmarshaller.unmarshal(new StringReader(xml));
        } catch (Exception e) {
            throw new IllegalStateException("Error parsing xml returned by MNB currency units query.", e);
        }
    }
}
