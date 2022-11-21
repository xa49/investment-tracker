package app.manager.transaction;

import app.broker.account.BrokerAccount;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TransactionMapper {
    default Long map(BrokerAccount brokerAccount) {
        if (brokerAccount == null) {
            return null;
        }
        return brokerAccount.getId();
    }

    @Mapping(source = "addToAccount", target = "addToAccountId")
    @Mapping(source = "takeFromAccount", target = "takeFromAccountId")
    TransactionDto toDto(Transaction transaction);

    List<TransactionDto> toDto(List<Transaction> transactions);
}
