package app.data.fx.currency;

import app.data.fx.currency.full_names.CurrencyNameService;
import app.data.fx.mnb_access.MNBCurrency;
import app.data.fx.mnb_access.MNBQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurrencyServiceBasicTest {

    @Mock
    BasicCurrencyRepository currencyRepository;

    @Mock
    MNBQueryService mnbQueryService;

    @Mock
    CurrencyNameService currencyNameService;

    @InjectMocks
    CurrencyServiceBasic currencyService;

    @Test
    void currencyWithCodeFoundLocally() {
        when(currencyRepository.findByIsoCode("JPY"))
                .thenReturn(Optional.of(new BasicCurrency("JPY", "Japanese Yen")));

        Optional<BasicCurrency> response = currencyService.getByCode("JPY");
        assertTrue(response.isPresent());
        assertEquals("Japanese Yen", response.get().getFullName());

        verify(mnbQueryService, never()).getCurrencyDetails(any());
    }

    @Test
    void currencyWithCodeFoundRemotelyWithName() {
        when(currencyRepository.findByIsoCode("JPY"))
                .thenReturn(Optional.empty());
        when(mnbQueryService.getCurrencyDetails("JPY"))
                .thenReturn(Optional.of(MNBCurrency.EMPTY_INSTANCE));
        when(currencyNameService.getCurrencyName("JPY"))
                .thenReturn("Japanese Yen");

        Optional<BasicCurrency> response = currencyService.getByCode("JPY");
        assertTrue(response.isPresent());
        assertEquals("Japanese Yen", response.get().getFullName());
    }

    @Test
    void currencyWithCodeFoundAtMNBButNotAtNameService() {
        when(currencyRepository.findByIsoCode("JPY"))
                .thenReturn(Optional.empty());
        when(mnbQueryService.getCurrencyDetails("JPY"))
                .thenReturn(Optional.of(MNBCurrency.EMPTY_INSTANCE));
        when(currencyNameService.getCurrencyName("JPY"))
                .thenThrow(new IllegalArgumentException("No full name found for currency: JPY"));

        Optional<BasicCurrency> response = currencyService.getByCode("JPY");
        assertTrue(response.isPresent());
        assertEquals("", response.get().getFullName());
    }

    @Test
    void currencyWithCodeNotFound() {
        when(currencyRepository.findByIsoCode("JPY"))
                .thenReturn(Optional.empty());
        when(mnbQueryService.getCurrencyDetails("JPY"))
                .thenReturn(Optional.empty());

        assertTrue(currencyService.getByCode("JPY").isEmpty());
        verify(currencyNameService, never()).getCurrencyName(any());
    }

    @Test
    void currencyIdFound() {
        when(currencyRepository.findById(34L))
                .thenReturn(Optional.of(new BasicCurrency("JPY", "Japanese Yen")));

        Optional<BasicCurrency> currency = currencyService.getById(34L);
        assertTrue(currency.isPresent());
        assertEquals("Japanese Yen", currency.get().getFullName());
    }

    @Test
    void currencyIdNotFound() {
        when(currencyRepository.findById(34L))
                .thenReturn(Optional.empty());

        Optional<BasicCurrency> currency = currencyService.getById(34L);
        assertTrue(currency.isEmpty());
    }
}