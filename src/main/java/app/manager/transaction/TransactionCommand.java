package app.manager.transaction;

import app.broker.fees.FeeType;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface TransactionCommand {
    LocalDate getDate();
     TransactionType getTransactionType();

     Long getTakeFromAccountId();
     String  getAssetTakenId();
     BigDecimal getCountOfAssetTaken();

     Long getAddToAccountId();
     String  getAssetAddedId();
     BigDecimal getCountOfAssetAdded();

     FeeType getFeeType();

     MatchingStrategy getMatchingStrategy();
}
