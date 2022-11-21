package app.data.securities.security;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "securities")
@Getter
@Setter
public class Security {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String ticker;
    @Column(nullable = false)
    private String fullName;

    private String currency;

    private String market;

    public Security() {
    }

    public Security(String ticker, String fullName, String market, String currency) {
        this.ticker = ticker;
        this.fullName = fullName;
        this.market = market;
        this.currency = currency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Security security = (Security) o;
        return Objects.equals(ticker, security.ticker) && Objects.equals(market, security.market);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ticker, market);
    }

    @Override
    public String toString() {
        return "Security{" +
                "ticker='" + ticker + '\'' +
                ", fullName='" + fullName + '\'' +
                ", market='" + market + '\'' +
                '}';
    }


}
