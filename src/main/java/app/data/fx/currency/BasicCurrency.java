package app.data.fx.currency;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "currencies")
@NoArgsConstructor
@Getter
@Setter
public class BasicCurrency implements Currency {
    @Id
    @GeneratedValue
    private Long id;
    @Column(unique = true)
    private String isoCode;
    private String fullName;

    public BasicCurrency(String isoCode, String fullName) {
        this.isoCode = isoCode;
        this.fullName = fullName;
    }

    @Override
    public String toString() {
        return "BasicCurrency{" +
                "id=" + id +
                ", isoCode='" + isoCode + '\'' +
                ", fullName='" + fullName + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BasicCurrency currency = (BasicCurrency) o;
        return Objects.equals(isoCode, currency.isoCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isoCode);
    }
}
