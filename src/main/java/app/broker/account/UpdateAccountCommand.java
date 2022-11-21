package app.broker.account;

import lombok.Data;

import java.time.LocalDate;

@Data
@ValidAccount
public class UpdateAccountCommand implements AccountCommand{
    private String name;
    private LocalDate openedDate;
    private LocalDate closedDate;
    private BrokerAccountType accountType;
}
