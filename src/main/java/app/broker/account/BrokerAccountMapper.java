package app.broker.account;

import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BrokerAccountMapper {
    BrokerAccountDto toDto(BrokerAccount brokerAccount);
    List<BrokerAccountDto> toDto(List<BrokerAccount> accounts);
}
