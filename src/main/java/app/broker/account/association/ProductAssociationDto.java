package app.broker.account.association;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ProductAssociationDto {
    private Long id;
    private Long accountId;
    private Long productId;
    private LocalDate fromDate;
    private LocalDate toDate;
}
