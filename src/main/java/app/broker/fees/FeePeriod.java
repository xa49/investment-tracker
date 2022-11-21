package app.broker.fees;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FeePeriod {
    DAILY,
    WEEKLY,
    EVERY_TWO_WEEKS,
    MONTHLY,
    QUARTERLY,
    ANNUAL

}
