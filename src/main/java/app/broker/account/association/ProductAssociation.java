package app.broker.account.association;

import app.broker.account.BrokerAccount;
import app.util.Dated;
import app.broker.product.BrokerProduct;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "account_product_associations")
@Getter
@Setter
@NoArgsConstructor
public class ProductAssociation implements Dated {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private BrokerAccount account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private BrokerProduct product;

    private LocalDate fromDate;
    private LocalDate toDate;

    public ProductAssociation(BrokerAccount account, BrokerProduct product, LocalDate fromDate, LocalDate toDate) {
        this.account = account;
        this.product = product;
        this.fromDate = fromDate;
        this.toDate = toDate;
    }
}
