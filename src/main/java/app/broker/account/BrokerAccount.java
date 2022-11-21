package app.broker.account;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "brokerage_accounts")
@Getter
@Setter
@NoArgsConstructor
public class BrokerAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private LocalDate openedDate;
    private LocalDate closedDate;

    @Enumerated(EnumType.STRING)
    private BrokerAccountType accountType;

    public void loadFromCommand(AccountCommand command) {
        name = command.getName();
        openedDate = command.getOpenedDate();
        closedDate = command.getClosedDate();
        accountType = command.getAccountType();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BrokerAccount account = (BrokerAccount) o;
        return Objects.equals(id, account.id) && Objects.equals(name, account.name)
                && Objects.equals(openedDate, account.openedDate) && Objects.equals(closedDate, account.closedDate)
                && accountType == account.accountType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, openedDate, closedDate, accountType);
    }

    @Override
    public String toString() {
        return "BrokerAccount{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", openedDate=" + openedDate +
                ", closedDate=" + closedDate +
                ", accountType=" + accountType +
                '}';
    }
}
