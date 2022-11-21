package app.manager.transaction;

import app.broker.fees.FeeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@Builder
@ValidTransaction
public class CreateTransactionCommand implements TransactionCommand {
    private LocalDate date;
    private TransactionType transactionType;

    private Long takeFromAccountId;
    private String assetTakenId;
    private BigDecimal countOfAssetTaken;

    private Long addToAccountId;
    private String assetAddedId;
    private BigDecimal countOfAssetAdded;

    private FeeType feeType;

    private MatchingStrategy matchingStrategy;
}
