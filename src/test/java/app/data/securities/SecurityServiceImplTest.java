package app.data.securities;

import app.data.securities.price.SecurityPrice;
import app.data.securities.price.SecurityPriceOfDay;
import app.data.securities.price.SecurityPriceOfDayRepository;
import app.data.securities.security.Security;
import app.data.securities.security.SecurityRepository;
import app.data.securities.yahoo_access.RemoteSecurityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityServiceImplTest {

    @Mock
    SecurityPriceOfDayRepository priceRepository;

    @Mock
    SecurityRepository securityRepository;

    @Mock
    RemoteSecurityService remoteSecurityService;

    @InjectMocks
    SecurityServiceImpl securityService;

    @Test
    void gettingLocallyAvailablePrice_withinTolerance() {
        when(securityRepository.countByTicker("MMM")).thenReturn(1);

        when(priceRepository.findByTickerAndDate("MMM", LocalDate.of(2000, 1, 1)))
                .thenReturn(Optional.empty());
        when(priceRepository.findByTickerAndDate("MMM", LocalDate.of(1999, 12, 31)))
                .thenReturn(Optional.empty());
        when(priceRepository.findByTickerAndDate("MMM", LocalDate.of(1999, 12, 30)))
                .thenReturn(Optional.of(SecurityPriceOfDay.builder()
                        .close(BigDecimal.TEN)
                        .date(LocalDate.of(1999, 12, 30))
                        .build()));
        when(remoteSecurityService.getPrices(eq("MMM"), any(), any()))
                .thenReturn(Collections.emptyMap());

        Optional<SecurityPrice> price = securityService.getPrice("MMM", LocalDate.of(2000, 1, 1), 2);
        assertTrue(price.isPresent());
        assertEquals(BigDecimal.TEN, price.get().getPrice());
        assertEquals(LocalDate.of(1999, 12, 30), price.get().getDate());
    }

    @Test
    void gettingRemotelyAvailablePrice_forExactDay() {
        when(securityRepository.countByTicker("MMM")).thenReturn(1);

        when(priceRepository.findByTickerAndDate("MMM", LocalDate.of(2000, 1, 1)))
                .thenReturn(Optional.empty());
        when(remoteSecurityService.getPrices(eq("MMM"), any(), any()))
                .thenReturn(Map.of(LocalDate.of(2000, 1, 1), SecurityPriceOfDay.builder().close(BigDecimal.TEN).build()));

        Optional<SecurityPrice> price = securityService.getPrice("MMM", LocalDate.of(2000, 1, 1));
        assertTrue(price.isPresent());
        assertEquals(BigDecimal.TEN, price.get().getPrice());
    }

    @Test
    void gettingRemotelyAvailablePrice_withinTolerance() {
        when(securityRepository.countByTicker("MMM")).thenReturn(1);

        when(priceRepository.findByTickerAndDate("MMM", LocalDate.of(2000, 1, 1)))
                .thenReturn(Optional.empty());
        when(remoteSecurityService.getPrices(eq("MMM"), any(), any()))
                .thenReturn(Map.of(LocalDate.of(1999, 12, 31), SecurityPriceOfDay.builder().close(BigDecimal.TEN).build()));
        // 1999-12-31 will be present as a result of saving new prices
        when(priceRepository.findByTickerAndDate("MMM", LocalDate.of(1999, 12, 31)))
                .thenReturn(Optional.ofNullable(SecurityPriceOfDay.builder().close(BigDecimal.TEN).build()));

        Optional<SecurityPrice> price = securityService.getPrice("MMM", LocalDate.of(2000, 1, 1), 2);
        assertTrue(price.isPresent());
        assertEquals(BigDecimal.TEN, price.get().getPrice());
    }

    @Test
    void priceIsNotAvailable() {
        when(securityRepository.countByTicker("MMM")).thenReturn(1);
        when(priceRepository.findByTickerAndDate(eq("MMM"), any()))
                .thenReturn(Optional.empty());
        when(remoteSecurityService.getPrices(eq("MMM"), any(), any()))
                .thenReturn(Collections.emptyMap());

        Optional<SecurityPrice> price = securityService.getPrice("MMM", LocalDate.of(2000, 1, 1), 20);
        assertTrue(price.isEmpty());
    }

    @Test
    void newRatesAreSaved() {
        when(securityRepository.countByTicker("MMM")).thenReturn(1);
        when(priceRepository.findByTickerAndDate("MMM", LocalDate.of(2000, 1, 1)))
                .thenReturn(Optional.empty());
        when(remoteSecurityService.getPrices(eq("MMM"), any(), any()))
                .thenReturn(Map.of(
                        LocalDate.of(1999, 12, 31), SecurityPriceOfDay.builder().close(BigDecimal.TEN).date(LocalDate.of(1999, 12, 31)).build(),
                        LocalDate.of(1999, 12, 30), SecurityPriceOfDay.builder().close(BigDecimal.TEN).date(LocalDate.of(1999, 12, 30)).build(),
                        LocalDate.of(1999, 12, 29), SecurityPriceOfDay.builder().close(BigDecimal.TEN).date(LocalDate.of(1999, 12, 29)).build()
                ));

        when(priceRepository.countByTickerAndDate(eq("MMM"), any()))
                .thenReturn(0L);
        when(priceRepository.countByTickerAndDate("MMM", LocalDate.of(1999, 12, 29)))
                .thenReturn(1L);

        securityService.getPrice("MMM", LocalDate.of(2000, 1, 1), 20);

        verify(priceRepository, times(1)).saveAll(argThat((List<SecurityPriceOfDay> s) -> s.size() == 2));
        verify(priceRepository, never()).saveAll(argThat((List<SecurityPriceOfDay> s) -> s.stream().anyMatch(p -> p.getDate().equals(LocalDate.of(1999, 12, 29)))));
    }

    @Test
    void remoteSourceCalledOnlyOnce() {
        when(securityRepository.countByTicker("MMM")).thenReturn(1);

        when(priceRepository.findByTickerAndDate(eq("MMM"), any()))
                .thenReturn(Optional.empty());
        when(remoteSecurityService.getPrices(eq("MMM"), any(), any()))
                .thenReturn(Collections.emptyMap());

        securityService.getPrice("MMM", LocalDate.of(2000, 1, 1), 200);

        verify(remoteSecurityService, times(1)).getPrices(any(), any(), any());
    }

    @Test
    void gettingLocallyAvailableSecurityByTicker() {
        when(securityRepository.findByTicker("MMM"))
                .thenReturn(Optional.of(new Security("MMM", "3M Corporation", "NYSE", "USD")));

        Optional<Security> security = securityService.getSecurityByTicker("MMM");
        assertTrue(security.isPresent());
        assertEquals("3M Corporation", security.get().getFullName());
    }

    @Test
    void gettingLocallyAvailableSecurityById() {
        when(securityRepository.findById(3L))
                .thenReturn(Optional.of(new Security("MMM", "3M Corporation", "NYSE", "USD")));

        Optional<Security> security = securityService.getSecurityById(3L);
        assertTrue(security.isPresent());
        assertEquals("3M Corporation", security.get().getFullName());
    }

    @Test
    void securityWithIdNotAvailable() {
        when(securityRepository.findById(3L))
                .thenReturn(Optional.empty());

        Optional<Security> security = securityService.getSecurityById(3L);
        assertTrue(security.isEmpty());
    }

    @Test
    void gettingRemotelyAvailableSecurity() {
        when(securityRepository.findByTicker("MMM"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(new Security("MMM", "3M Corporation", "NYSE", "USD")));

        when(remoteSecurityService.getSecurityDetails("MMM"))
                .thenReturn(Optional.of(new Security("MMM", "3M Corporation", "NYSE", "USD")));

        Optional<Security> security = securityService.getSecurityByTicker("MMM");
        assertTrue(security.isPresent());
        assertEquals("3M Corporation", security.get().getFullName());
    }

    @Test
    void gettingUnavailableSecurity() {
        when(securityRepository.findByTicker("MMM"))
                .thenReturn(Optional.empty());

        when(remoteSecurityService.getSecurityDetails("MMM"))
                .thenReturn(Optional.empty());

        Optional<Security> security = securityService.getSecurityByTicker("MMM");
        assertTrue(security.isEmpty());
    }

    @Test
    void newSecuritySaved() {
        when(securityRepository.findByTicker("MMM"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(new Security("MMM", "3M Corporation", "NYSE", "USD")));


        when(remoteSecurityService.getSecurityDetails("MMM"))
                .thenReturn(Optional.of(new Security("MMM", "3M Corporation", "NYSE", "USD")));

        securityService.getSecurityByTicker("MMM");
        verify(securityRepository).save(argThat(s -> s.getTicker().equals("MMM")));
    }
}