package app.util;

import java.time.LocalDate;

public interface Dated {
    LocalDate getFromDate();
    LocalDate getToDate();
    void setFromDate(LocalDate date);
    void setToDate(LocalDate date);
}
