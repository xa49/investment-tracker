package app.data.fx.currency.full_names;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

@Service
@Slf4j
public class CurrencyNameService {

    private static final String FULL_CURRENCY_NAME_API_URL = "https://openexchangerates.org/api/currencies.json";

    public String getCurrencyName(String currencyCode) {
        JsonObject currencies = JsonParser.parseString(getRequestResponse()).getAsJsonObject();

        try {
            return currencies.get(currencyCode).getAsString();
        } catch (NullPointerException e) {
            throw new IllegalArgumentException("No full name found for currency: " + currencyCode);
        }
    }

    private String getRequestResponse() {
        log.info("Requesting full currency names from {}", FULL_CURRENCY_NAME_API_URL);
        try {
            URL url = new URL(FULL_CURRENCY_NAME_API_URL);
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            return extractResponse(con);
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException("No response from full currency name API.");
        }
    }

    private  String extractResponse(HttpsURLConnection con) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            String inputLine;
            StringBuilder response = new StringBuilder();
            while (((inputLine = reader.readLine()) != null)) {
                response.append(inputLine);
            }
            return response.toString();
        }
    }
}
