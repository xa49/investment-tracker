package app.analysis.portfolio;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Set;

@Entity
@Table(name = "portfolios")
@Getter
@Setter
@NoArgsConstructor
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @ElementCollection
    @CollectionTable(name = "portfolio_memberships", joinColumns = @JoinColumn(name = "portfolio_id"))
    @Column(name = "account_id")
    private Set<Long> accountIds;

    public Portfolio(String name, Set<Long> accountIds) {
        this.name = name;
        this.accountIds = accountIds;
    }
}
