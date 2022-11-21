package app.broker.account;

import java.time.LocalDate;

public interface AccountCommand {

     String getName();
     LocalDate getOpenedDate();
     LocalDate getClosedDate();

     BrokerAccountType getAccountType();
}
