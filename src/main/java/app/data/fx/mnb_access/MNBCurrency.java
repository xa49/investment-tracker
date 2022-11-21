package app.data.fx.mnb_access;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.*;

@XmlRootElement(name = "MNBCurrencyUnits")
@XmlAccessorType(XmlAccessType.FIELD)
@NoArgsConstructor
@AllArgsConstructor
public class MNBCurrency {
    public static final MNBCurrency EMPTY_INSTANCE =
            new MNBCurrency(new MNBCurrencyWrapper(new MNBCurrencyDetails("", 0)));

    @XmlElement(name = "Units")
    private MNBCurrencyWrapper mnbCurrencyWrapper;

    boolean isPresent() {
        return mnbCurrencyWrapper != null && mnbCurrencyWrapper.mnbCurrencyDetails != null
                && mnbCurrencyWrapper.mnbCurrencyDetails.currency != null
                && mnbCurrencyWrapper.mnbCurrencyDetails.unit != 0;
    }

    public String getCurrency() {
        return mnbCurrencyWrapper.mnbCurrencyDetails.currency;
    }

    public int getUnits() {
        return mnbCurrencyWrapper.mnbCurrencyDetails.unit;
    }


    @NoArgsConstructor
    @AllArgsConstructor
    static class MNBCurrencyWrapper {
        @XmlElement(name = "Unit")
        private MNBCurrencyDetails mnbCurrencyDetails;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    static class MNBCurrencyDetails {
        @XmlAttribute(name = "curr")
        private String currency;

        @XmlValue
        private int unit;
    }
}
