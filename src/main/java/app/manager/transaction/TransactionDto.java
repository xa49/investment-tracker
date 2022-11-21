package app.manager.transaction;

import app.broker.fees.FeeType;
import app.manager.transaction.asset_record.InvestmentAssetRecord;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TransactionDto {
    private Long id;
    private LocalDate date;
    private TransactionType transactionType;

    private Long takeFromAccountId;
    private InvestmentAssetRecord assetTaken;
    private BigDecimal countOfAssetTaken;

    private Long addToAccountId;
    private InvestmentAssetRecord  assetAdded;
    private BigDecimal countOfAssetAdded;

    private BigDecimal feeAmount;
    private String feeCurrency;
    private FeeType feeType;

    private MatchingStrategy matchingStrategy;
}
