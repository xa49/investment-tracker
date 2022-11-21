package app.broker.account.association;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidAssociation
public class CreateProductAssociationCommand implements ProductAssociationCommand{
    private Long productId;
    private LocalDate fromDate;
    private LocalDate toDate;
}
