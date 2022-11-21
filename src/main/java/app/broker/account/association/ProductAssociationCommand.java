package app.broker.account.association;

import java.time.LocalDate;

public interface ProductAssociationCommand {
     Long getProductId();
     LocalDate getFromDate();
     LocalDate getToDate();
}
