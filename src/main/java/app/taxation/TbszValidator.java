package app.taxation;

import app.broker.account.BrokerAccountDto;
import app.broker.account.BrokerAccountService;
import app.broker.account.BrokerAccountType;
import app.util.InvalidDataException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@AllArgsConstructor
public class TbszValidator {
    private final BrokerAccountService accountService;

    public void validateForTbszEntryConstraint(LocalDate date, Long accountId) {
        BrokerAccountDto account = accountService.getById(accountId);
        if (account.getAccountType() == BrokerAccountType.TBSZ
                && (account.getOpenedDate() == null || account.getOpenedDate().getYear() != date.getYear())) {
            throw new InvalidDataException(
                    "Trying to add transaction to TBSZ account outside collection year. Account: "
                            + account + ". Date: " + date);
        }
    }
}
