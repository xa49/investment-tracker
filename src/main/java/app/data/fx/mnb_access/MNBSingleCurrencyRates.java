package app.data.fx.mnb_access;

import javax.xml.bind.annotation.*;
import java.util.List;

@XmlRootElement(name = "MNBExchangeRates")
@XmlAccessorType(XmlAccessType.FIELD)
public class MNBSingleCurrencyRates {

    @XmlElement(name = "Day")
    private List<MNBSingleCurrencyDay> rates;

    public List<MNBSingleCurrencyDay> getRates() {
        return rates;
    }

    public boolean isPresent() {
        return rates != null && !rates.isEmpty() && rates.get(0) != null && rates.get(0).mnbRate != null;
    }

    static class MNBSingleCurrencyDay {
        @XmlAttribute
        private String date;

        @XmlElement(name = "Rate")
        private MNBRate mnbRate;

        public String getDate() {
            return date;
        }

        public String getPresentationRate() {
            return mnbRate.presentationRate;
        }

        public int getPresentationUnits() {
            return mnbRate.presentationUnits;
        }
    }

    static class MNBRate {
        @XmlValue
        private String presentationRate;

        @XmlAttribute(name = "unit")
        private int presentationUnits;
    }
}
