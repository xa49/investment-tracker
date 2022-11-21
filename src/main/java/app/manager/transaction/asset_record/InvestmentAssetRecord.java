package app.manager.transaction.asset_record;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "investment_asset_records")
@Getter
@Setter
@NoArgsConstructor
public class InvestmentAssetRecord {

    @Id
    @GeneratedValue(strategy =  GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private InvestmentAssetType type;
    private Long assetId;

    public InvestmentAssetRecord(InvestmentAssetType type, Long assetId) {
        this.type = type;
        this.assetId = assetId;
    }
}
