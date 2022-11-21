package app.broker.account;

import lombok.Data;

import java.time.LocalDate;

@Data
public class BrokerAccountDto {
    private Long id;
    private String name;
    private LocalDate openedDate;
    private LocalDate closedDate;
    private BrokerAccountType accountType;
}
