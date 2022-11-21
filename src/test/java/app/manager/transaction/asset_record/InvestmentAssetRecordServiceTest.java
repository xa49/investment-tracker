package app.manager.transaction.asset_record;

import app.data.DataService;
import app.data.fx.currency.BasicCurrency;
import app.data.securities.security.Security;
import app.util.InvalidDataException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvestmentAssetRecordServiceTest {

    @Mock
    InvestmentAssetRecordRepository assetRecordRepository;

    @Mock
    DataService dataService;

    @InjectMocks
    InvestmentAssetRecordService recordService;

    @Test
    void gettingExistingCashRecord() {
        BasicCurrency currency = new BasicCurrency("EUR", "Euro");
        currency.setId(10L);
        when(dataService.getCurrencyDetailsByCode("EUR"))
                .thenReturn(currency);

        when(assetRecordRepository.findCashRecordById(10L))
                .thenReturn(Optional.of(new InvestmentAssetRecord(InvestmentAssetType.CASH, 10L)));

        InvestmentAssetRecord cashRecord = recordService.getCashRecord("EUR");
        assertEquals(InvestmentAssetType.CASH, cashRecord.getType());
        assertEquals(10L, cashRecord.getAssetId());
    }

    @Test
    void gettingNewlyAddedCashRecord() {
        BasicCurrency currency = new BasicCurrency("EUR", "Euro");
        currency.setId(10L);
        when(dataService.getCurrencyDetailsByCode("EUR"))
                .thenReturn(currency);

        when(assetRecordRepository.findCashRecordById(10L))
                .thenReturn(Optional.empty());

        InvestmentAssetRecord cashRecord = recordService.getCashRecord("EUR");
        assertEquals(InvestmentAssetType.CASH, cashRecord.getType());
        assertEquals(10L, cashRecord.getAssetId());
        verify(assetRecordRepository).save(argThat(r -> r.getAssetId() == 10L));
    }

    @Test
    void gettingRecordForInvalidCurrency() {
        when(dataService.getCurrencyDetailsByCode("EURQQQ"))
                .thenThrow(new InvalidDataException("Invalid currency"));

        InvalidDataException ex = assertThrows(InvalidDataException.class,
                () -> recordService.getCashRecord("EURQQQ"));
        assertEquals("Invalid currency", ex.getMessage());
    }

    @Test
    void gettingExistingSecurityRecord() {
        Security security = new Security("MMM", "3M Corporation", "NYSE", "USD");
        security.setId(11L);
        when(dataService.getSecurityDetailsByTicker("MMM"))
                .thenReturn(security);
        when(assetRecordRepository.findSecurityRecordById(11L))
                .thenReturn(Optional.of(new InvestmentAssetRecord(InvestmentAssetType.SECURITY, 11L)));

        InvestmentAssetRecord securityRecord = recordService.getSecurityRecord("MMM");
        assertEquals(11L, securityRecord.getAssetId());
        assertEquals(InvestmentAssetType.SECURITY, securityRecord.getType());
    }

    @Test
    void gettingNewlyAddedSecurityRecord() {
        Security security = new Security("MMM", "3M Corporation", "NYSE", "USD");
        security.setId(11L);
        when(dataService.getSecurityDetailsByTicker("MMM"))
                .thenReturn(security);
        when(assetRecordRepository.findSecurityRecordById(11L))
                .thenReturn(Optional.empty());

        InvestmentAssetRecord securityRecord = recordService.getSecurityRecord("MMM");
        assertEquals(11L, securityRecord.getAssetId());
        assertEquals(InvestmentAssetType.SECURITY, securityRecord.getType());
        verify(assetRecordRepository).save(argThat(r -> r.getAssetId() == 11L));
    }

    @Test
    void gettingRecordForInvalidSecurity() {
        when(dataService.getSecurityDetailsByTicker("MMM111"))
                .thenThrow(new InvalidDataException("Invalid ticker"));

        InvalidDataException ex = assertThrows(InvalidDataException.class,
                () -> recordService.getSecurityRecord("MMM111"));
        assertEquals("Invalid ticker", ex.getMessage());
    }
}