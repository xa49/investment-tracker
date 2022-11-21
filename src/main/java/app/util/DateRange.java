package app.util;

import java.time.LocalDate;

public record DateRange(Long id, LocalDate fromDate, LocalDate toDate) {
    @Override
    public String toString() {
        return (fromDate != null ? fromDate : "")
                + "--"
                + (toDate != null ? toDate : "")
                + " (id: " + id + ")";
    }
}