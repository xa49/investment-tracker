package app.broker.account.association;

import lombok.Data;

import java.time.LocalDate;

@Data
@ValidAssociation
public class UpdateProductAssociationCommand implements ProductAssociationCommand{
    private Long productId;
    private LocalDate fromDate;
    private LocalDate toDate;
}
