package app.analysis.actual;

import lombok.Getter;

import java.time.LocalDate;

@Getter
public class GetActualPositionCommand {
    private String portfolioName;
    private String taxResidence;
    private LocalDate asOfDate;
}
