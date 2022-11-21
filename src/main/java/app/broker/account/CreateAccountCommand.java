package app.broker.account;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidAccount
public class CreateAccountCommand implements AccountCommand {
    private String name;
    private LocalDate openedDate;
    private LocalDate closedDate;
    private BrokerAccountType accountType;
}
