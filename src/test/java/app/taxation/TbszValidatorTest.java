package app.taxation;

import app.broker.account.BrokerAccountDto;
import app.broker.account.BrokerAccountService;
import app.broker.account.BrokerAccountType;
import app.util.InvalidDataException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TbszValidatorTest {

    @Mock
    BrokerAccountService accountService;

    @InjectMocks
    TbszValidator tbszValidator;

    @Test
    void ongoingValidTbsz() {
        BrokerAccountDto account = new BrokerAccountDto();
        account.setAccountType(BrokerAccountType.TBSZ);
        account.setOpenedDate(LocalDate.EPOCH);
        when(accountService.getById(1L))
                .thenReturn(account);
        assertDoesNotThrow(() -> tbszValidator.validateForTbszEntryConstraint(LocalDate.EPOCH, 1L));
    }

    @Test
    void notValid_accountOpenDateNotSet() {
        BrokerAccountDto account = new BrokerAccountDto();
        account.setAccountType(BrokerAccountType.TBSZ);
        when(accountService.getById(1L))
                .thenReturn(account);
        InvalidDataException ex = assertThrows(InvalidDataException.class,
                () -> tbszValidator.validateForTbszEntryConstraint(LocalDate.EPOCH, 1L));
        assertEquals("Trying to add transaction to TBSZ account outside collection year. Account: BrokerAccountDto(id=null, name=null, openedDate=null, closedDate=null, accountType=TBSZ). Date: 1970-01-01",
                ex.getMessage());
    }

    @Test
    void notValid_accountOpenedDateInPreviousYear() {
        BrokerAccountDto account = new BrokerAccountDto();
        account.setAccountType(BrokerAccountType.TBSZ);
        account.setOpenedDate(LocalDate.EPOCH.minusYears(1));
        when(accountService.getById(1L))
                .thenReturn(account);
        InvalidDataException ex = assertThrows(InvalidDataException.class,
                () -> tbszValidator.validateForTbszEntryConstraint(LocalDate.EPOCH, 1L));
        assertEquals("Trying to add transaction to TBSZ account outside collection year. Account: BrokerAccountDto(id=null, name=null, openedDate=1969-01-01, closedDate=null, accountType=TBSZ). Date: 1970-01-01",
                ex.getMessage());
    }

    @Test
    void accountNotTbsz_shouldNotThrow() {
        BrokerAccountDto account = new BrokerAccountDto();
        account.setAccountType(BrokerAccountType.MAIN);
        account.setOpenedDate(LocalDate.EPOCH);
        when(accountService.getById(1L))
                .thenReturn(account);
        assertDoesNotThrow(() -> tbszValidator.validateForTbszEntryConstraint(LocalDate.EPOCH, 1L));
    }

}