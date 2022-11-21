package app.data.fx.rate;

import app.data.fx.mnb_access.MNBQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceMnbTest {

    @Mock
    ExchangeRateRepository exchangeRateRepository;

    @Mock
    MNBQueryService mnbQueryService;

    @InjectMocks
    ExchangeRateServiceMnb exchangeRateService;


    @Test
    void sameCurrencyRateEqualsOne_andEfficient() {
        Rate exchangeRate = exchangeRateService.getExchangeRateDetails("KWD", "KWD",
                LocalDate.of(2000, 1, 1)).orElseThrow();
        assertEquals(BigDecimal.ONE, exchangeRate.getExchangeRate());
        assertEquals(LocalDate.of(2000, 1, 1), exchangeRate.getDate());
        verify(exchangeRateRepository, never()).findRate(any(), any(), any());
    }

    @Test
    void exactDayRateWanted_againstHUF_haveLocally() {
        when(exchangeRateRepository.findRate("EUR", "HUF",
                LocalDate.of(2000, 1, 1)))
                .thenReturn(Optional.of(new StoredRate("EUR", "HUF",
                        LocalDate.of(2000, 1, 1), BigDecimal.TEN)));

        Optional<Rate> exchangeRate = exchangeRateService.getExchangeRateDetails("EUR", "HUF",
                LocalDate.of(2000, 1, 1));
        assertTrue(exchangeRate.isPresent());
        assertEquals(BigDecimal.TEN, exchangeRate.get().getExchangeRate());
        assertEquals("EUR", exchangeRate.get().getSourceIsoAbbreviation());
    }

    @Test
    void bothCurrenciesAvailableLocally() {
        when(exchangeRateRepository.findRate("EUR", "HUF",
                LocalDate.of(2000, 1, 1)))
                .thenReturn(Optional.of(new StoredRate("EUR", "HUF",
                        LocalDate.of(2000, 1, 1), new BigDecimal("220"))));
        when(exchangeRateRepository.findRate("USD", "HUF",
                LocalDate.of(2000, 1, 1)))
                .thenReturn(Optional.of(new StoredRate("USD", "HUF",
                        LocalDate.of(2000, 1, 1), new BigDecimal("200"))));

        Optional<Rate> exchangeRate = exchangeRateService.getExchangeRateDetails("EUR", "USD",
                LocalDate.of(2000, 1, 1));
        assertTrue(exchangeRate.isPresent());
        assertEquals(new BigDecimal("1.1"), exchangeRate.get().getExchangeRate());
    }

    @Test
    void oneCurrencyLocally_oneCurrencyRemotelyAvailable_forExactDate() {
        // available locally
        when(exchangeRateRepository.findRate("EUR", "HUF",
                LocalDate.of(2000, 1, 1)))
                .thenReturn(Optional.of(new StoredRate("EUR", "HUF",
                        LocalDate.of(2000, 1, 1), new BigDecimal("220"))));

        // available remotely
        when(exchangeRateRepository.findRate("USD", "HUF",
                LocalDate.of(2000, 1, 1)))
                // initially not available locally
                .thenReturn(Optional.empty())
                // then saved for this day as a result of a call to MNBQueryService
                .thenReturn(
                        Optional.of(new StoredRate("USD", "HUF",
                                LocalDate.of(2000, 1, 1), new BigDecimal("200"))));
        when(mnbQueryService.getHufRates(eq("USD"), any(), eq(LocalDate.of(2000, 1, 1))))
                .thenReturn(Map.of(
                        LocalDate.of(1999, 12, 31), new BigDecimal("190"),
                        LocalDate.of(2000, 1, 1), new BigDecimal("200")));

        Optional<Rate> exchangeRate = exchangeRateService.getExchangeRateDetails("EUR", "USD",
                LocalDate.of(2000, 1, 1));
        assertTrue(exchangeRate.isPresent());
        assertEquals(new BigDecimal("1.1"), exchangeRate.get().getExchangeRate());
        verify(exchangeRateRepository, atLeastOnce()).save(argThat(r -> r.getSourceIsoAbbreviation().equals("USD")));
    }

    @Test
    void allRemoteDataSavedIfNotAvailable() {
        when(exchangeRateRepository.findRate(eq("EUR"), eq("HUF"),
                any()))
                .thenReturn(Optional.of(new StoredRate("EUR", "HUF",
                        LocalDate.of(2000, 1, 1), BigDecimal.ONE)));

        // USD rates are not available locally, except 1999-12-29
        when(exchangeRateRepository.findRate(eq("USD"), eq("HUF"),
                any())).thenReturn(Optional.empty());
        when(mnbQueryService.getHufRates(eq("USD"), any(), eq(LocalDate.of(2000, 1, 1))))
                .thenReturn(Map.of(
                        LocalDate.of(1999, 12, 28), new BigDecimal("200"),
                        LocalDate.of(1999, 12, 29), new BigDecimal("200"),
                        LocalDate.of(1999, 12, 30), new BigDecimal("200")));

        when(exchangeRateRepository.isAlreadyStored("USD", "HUF", LocalDate.of(1999, 12, 29)))
                .thenReturn(true);
        when(exchangeRateRepository.isAlreadyStored("USD", "HUF", LocalDate.of(1999, 12, 28)))
                .thenReturn(false);
        when(exchangeRateRepository.isAlreadyStored("USD", "HUF", LocalDate.of(1999, 12, 30)))
                .thenReturn(false);


        // when
        exchangeRateService.getExchangeRateDetails("EUR", "USD",
                LocalDate.of(2000, 1, 1));

        // save method called for each day not present
        verify(exchangeRateRepository, times(2)).save(any());
        verify(exchangeRateRepository, never()).save(argThat(r -> r.getDate().getDayOfMonth() == 29));
    }

    @Test
    void callToRemoteServiceOnlyOnce() {
        when(exchangeRateRepository.findRate(eq("EUR"), eq("HUF"),
                any())).thenReturn(Optional.of(new StoredRate("EUR", "HUF",
                LocalDate.of(2000, 1, 1), BigDecimal.ONE)));

        when(exchangeRateRepository.findRate("USD", "HUF",
                LocalDate.of(2000, 1, 1)))
                .thenReturn(Optional.empty());
        when(mnbQueryService.getHufRates(eq("USD"), any(), eq(LocalDate.of(2000, 1, 1))))
                .thenReturn(Collections.emptyMap());

        exchangeRateService.getExchangeRateDetails("EUR", "USD",
                LocalDate.of(2000, 1, 1), 500);

        verify(mnbQueryService, times(1)).getHufRates(any(), any(), any());
    }

    @Test
    void oneCurrencyLocally_otherCurrencyNotAvailableForExactDay() {
        // available locally
        when(exchangeRateRepository.findRate("EUR", "HUF",
                LocalDate.of(2000, 1, 1)))
                .thenReturn(Optional.of(new StoredRate("EUR", "HUF",
                        LocalDate.of(2000, 1, 1), new BigDecimal("220"))));

        // only prior day available remotely
        when(exchangeRateRepository.findRate("USD", "HUF",
                LocalDate.of(2000, 1, 1)))
                .thenReturn(Optional.empty());
        when(mnbQueryService.getHufRates(eq("USD"), any(), eq(LocalDate.of(2000, 1, 1))))
                .thenReturn(
                        Map.of(
                                LocalDate.of(1999, 12, 31), new BigDecimal("190")));

        Optional<Rate> exchangeRate = exchangeRateService.getExchangeRateDetails("EUR", "USD",
                LocalDate.of(2000, 1, 1));
        assertTrue(exchangeRate.isEmpty());
    }

    @Test
    void bothCurrenciesRemotelyAvailable_atLastOffsetToleratedDay() {
        when(exchangeRateRepository.findRate("EUR", "HUF",
                LocalDate.of(2000, 1, 1)))
                .thenReturn(Optional.empty()) // initially not available locally
                .thenReturn( // added after call to MNBQueryService
                        Optional.of(new StoredRate("EUR", "HUF",
                                LocalDate.of(1999, 12, 30), new BigDecimal("220"))));
        when(mnbQueryService.getHufRates(eq("EUR"), any(), eq(LocalDate.of(2000, 1, 1))))
                .thenReturn(Map.of(
                        LocalDate.of(2000,1,1), new BigDecimal("220"), // exact date only available for EUR
                        LocalDate.of(1999, 12, 30), new BigDecimal("220")));

        when(exchangeRateRepository.findRate("USD", "HUF",
                LocalDate.of(2000, 1, 1)))
                .thenReturn(Optional.empty())
                .thenReturn(
                        Optional.of(new StoredRate("USD", "HUF",
                                LocalDate.of(1999, 12, 30), new BigDecimal("200"))));
        when(mnbQueryService.getHufRates(eq("USD"), any(), eq(LocalDate.of(2000, 1, 1))))
                .thenReturn(Map.of(
                        LocalDate.of(1999, 12, 30), new BigDecimal("200")));

        Optional<Rate> exchangeRate = exchangeRateService.getExchangeRateDetails("EUR", "USD",
                LocalDate.of(2000, 1, 1), 2);
        assertTrue(exchangeRate.isPresent());
        assertEquals(new BigDecimal("1.1"), exchangeRate.get().getExchangeRate());
        assertEquals(LocalDate.of(1999, 12, 30), exchangeRate.get().getDate());
    }

    @Test
    void bothCurrenciesRemotelyAvailable_butOutsideOffsetTolerance() {
        when(exchangeRateRepository.findRate("EUR", "HUF",
                LocalDate.of(2000, 1, 1)))
                .thenReturn(Optional.empty());
        when(mnbQueryService.getHufRates(eq("EUR"), any(), eq(LocalDate.of(2000, 1, 1))))
                .thenReturn(
                        Map.of(
                                LocalDate.of(1999, 12, 30), new BigDecimal("220")));

        when(exchangeRateRepository.findRate("USD", "HUF",
                LocalDate.of(2000, 1, 1)))
                .thenReturn(Optional.empty());
        when(mnbQueryService.getHufRates(eq("USD"), any(), eq(LocalDate.of(2000, 1, 1))))
                .thenReturn(
                        Map.of(
                                LocalDate.of(1999, 12, 30), new BigDecimal("190")));

        Optional<Rate> exchangeRate = exchangeRateService.getExchangeRateDetails("EUR", "USD",
                LocalDate.of(2000, 1, 1), 1);
        assertTrue(exchangeRate.isEmpty());
    }

}