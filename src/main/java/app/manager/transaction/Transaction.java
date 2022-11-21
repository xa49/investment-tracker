package app.manager.transaction;

import app.broker.account.BrokerAccount;
import app.broker.fees.FeeType;
import app.manager.transaction.asset_record.InvestmentAssetRecord;
import app.util.BigDecimalConverter;
import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "take_account_id")
    private BrokerAccount takeFromAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_taken_record_id")
    private InvestmentAssetRecord assetTaken;

    @Convert(converter = BigDecimalConverter.class)
    private BigDecimal countOfAssetTaken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "add_account_id")
    private BrokerAccount addToAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_added_record_id")
    private InvestmentAssetRecord assetAdded;

    @Convert(converter = BigDecimalConverter.class)
    private BigDecimal countOfAssetAdded;

    @Convert(converter = BigDecimalConverter.class)
    private BigDecimal feeAmount;
    private String feeCurrency;
    @Enumerated(EnumType.STRING)
    private FeeType feeType;

    @Enumerated(EnumType.STRING)
    private MatchingStrategy matchingStrategy;

    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;

    public Transaction(Builder builder) {
        this.date = builder.date;
        this.transactionType = builder.transactionType;

        this.countOfAssetAdded = builder.countOfAssetAdded;
        this.assetAdded = builder.assetAdded;
        this.addToAccount = builder.addToAccount;

        this.countOfAssetTaken = builder.countOfAssetTaken;
        this.assetTaken = builder.assetTaken;
        this.takeFromAccount = builder.takeFromAccount;

        this.feeAmount = builder.feeAmount;
        this.feeCurrency = builder.feeCurrency;
        this.feeType = builder.feeType;

        this.matchingStrategy = builder.matchingStrategy;
    }

    public static Builder builder(LocalDate date, TransactionType type) {
        return new Builder(date, type);
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", date=" + date +
                ", takeFromAccount=" + takeFromAccount +
                ", assetTaken=" + assetTaken +
                ", countOfAssetTaken=" + countOfAssetTaken +
                ", addToAccount=" + addToAccount +
                ", assetAdded=" + assetAdded +
                ", countOfAssetAdded=" + countOfAssetAdded +
                ", feeAmount=" + feeAmount +
                ", feeCurrency='" + feeCurrency + '\'' +
                ", feeType=" + feeType +
                ", matchingStrategy=" + matchingStrategy +
                ", transactionType=" + transactionType +
                '}';
    }


    public static class Builder {
        private final LocalDate date;
        private final TransactionType transactionType;
        private BrokerAccount takeFromAccount;
        private InvestmentAssetRecord assetTaken;
        private BigDecimal countOfAssetTaken;
        private BrokerAccount addToAccount;
        private InvestmentAssetRecord assetAdded;
        private BigDecimal countOfAssetAdded;
        private BigDecimal feeAmount;
        private String feeCurrency;
        private FeeType feeType;
        private MatchingStrategy matchingStrategy;

        public Builder(LocalDate date, TransactionType type) {
            this.date = date;
            this.transactionType = type;
        }

        public Builder add(BigDecimal amount, InvestmentAssetRecord asset, BrokerAccount account) {
            this.countOfAssetAdded = amount;
            this.assetAdded = asset;
            this.addToAccount = account;
            return this;
        }

        public Builder take(BigDecimal amount, InvestmentAssetRecord asset, BrokerAccount account) {
            this.countOfAssetTaken = amount;
            this.assetTaken = asset;
            this.takeFromAccount = account;
            return this;
        }

        public Builder fee(BigDecimal amount, String currency, FeeType type) {
            this.feeAmount = amount;
            this.feeCurrency = currency;
            this.feeType = type;
            return this;
        }

        public Builder matching(MatchingStrategy matchingStrategy) {
            this.matchingStrategy = matchingStrategy;
            return this;
        }

        public Transaction build() {
            return new Transaction(this);
        }

    }
}
