package app.broker;

import app.util.DateRange;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
public class OverlappingDetail {
    private boolean issue;
    private String message;

    public OverlappingDetail(LocalDate providedFrom, LocalDate providedTo, List<DateRange> issues) {
        issue = true;
        message = "You provided the range "
                + (providedFrom != null ? providedFrom : "" )
                + " - "
                + (providedTo != null ? providedTo : "")
                + " but these periods overlap with it: " + issues;
    }
}
