package app.analysis.liquid;

import lombok.Getter;

import java.time.LocalDate;

@Getter
public class GetLiquidValueCommand {
    private String portfolioName;
    private String  currency;
    private String taxResidence;
    private LocalDate asOfDate;
}
