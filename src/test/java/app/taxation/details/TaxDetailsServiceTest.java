package app.taxation.details;

import app.broker.BrokerEntityNotFoundException;
import app.broker.UniqueViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaxDetailsServiceTest {


    @Mock
    TaxDetailsRepository taxDetailsRepository;

    @Spy
    TaxDetailMapperImpl mapper;

    @InjectMocks
    TaxDetailsService taxDetailsService;

    @Test
    void listTaxDetails() {
        TaxDetails taxDetails1 = new TaxDetails();
        TaxDetails taxDetails2 = new TaxDetails();
        taxDetails1.setTaxationCurrency("HUF");
        taxDetails2.setTaxationCurrency("EUR");

        when(taxDetailsRepository.findAll())
                .thenReturn(List.of(taxDetails1, taxDetails2));

        List<TaxDetailsDto> taxDetailsDtos = taxDetailsService.listTaxDetails();
        assertEquals(List.of("HUF", "EUR"), taxDetailsDtos.stream().map(TaxDetailsDto::getTaxationCurrency).toList());
    }

    @Test
    void addTaxDetails_notOverlapping() {
        CreateTaxDetailsCommand command = CreateTaxDetailsCommand.builder()
                .flatCapitalGainsTaxRate(BigDecimal.ONE)
                        .taxationCurrency("HUF").build();

        when(taxDetailsRepository.findForResidenceAndDate(null, null, null))
                .thenReturn(Collections.emptyList());

        TaxDetailsDto dto = taxDetailsService.addTaxDetails(command);
        assertEquals(BigDecimal.ONE, dto.getFlatCapitalGainsTaxRate());
        assertEquals("HUF", dto.getTaxationCurrency());

        verify(taxDetailsRepository).save(argThat(d -> d.getFlatCapitalGainsTaxRate().compareTo(BigDecimal.ONE) == 0
                && d.getTaxationCurrency().equals("HUF")));
    }

    @Test
    void addTaxDetails_overlapping() {
        CreateTaxDetailsCommand command = CreateTaxDetailsCommand.builder()
                .flatCapitalGainsTaxRate(BigDecimal.ONE)
                .taxResidence("HU").build();

        TaxDetails taxDetails = new TaxDetails();
        taxDetails.setId(4L);
        taxDetails.setFromDate(LocalDate.EPOCH);
        when(taxDetailsRepository.findForResidenceAndDate("HU", null, null))
                .thenReturn(List.of(taxDetails));

        UniqueViolationException ex = assertThrows(UniqueViolationException.class,
                () -> taxDetailsService.addTaxDetails(command));
        assertEquals("Existing tax details for HU overlap with the specified period (null-null). Overlapping ids: [4]",
                ex.getMessage());
    }

    @Test
    void updateTaxDetails_valid() {
        UpdateTaxDetailsCommand command = new UpdateTaxDetailsCommand();
        command.setFlatCapitalGainsTaxRate(BigDecimal.ONE);
        command.setTaxResidence("HU");

        when(taxDetailsRepository.findById(4L))
                .thenReturn(Optional.of(new TaxDetails()));
        when(taxDetailsRepository.findOthersForResidenceAndDate("HU", null, null, 4L))
                .thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> taxDetailsService.updateTaxDetails(4L, command));
    }

    @Test
    void updateTaxDetails_notFound() {
        UpdateTaxDetailsCommand command = new UpdateTaxDetailsCommand();
        command.setFlatCapitalGainsTaxRate(BigDecimal.ONE);
        command.setTaxResidence("HU");


        BrokerEntityNotFoundException ex = assertThrows(BrokerEntityNotFoundException.class,
                () -> taxDetailsService.updateTaxDetails(4L, command));
        assertEquals("No tax detail with id: 4", ex.getMessage());
    }

    @Test
    void updateTaxDetails_overlapping() {
        UpdateTaxDetailsCommand command = new UpdateTaxDetailsCommand();
        command.setFlatCapitalGainsTaxRate(BigDecimal.ONE);
        command.setTaxResidence("HU");

        TaxDetails taxDetails = new TaxDetails();
        taxDetails.setId(5L);
        taxDetails.setFromDate(LocalDate.EPOCH);
        when(taxDetailsRepository.findById(4L))
                .thenReturn(Optional.of(new TaxDetails()));
        when(taxDetailsRepository.findOthersForResidenceAndDate("HU", null, null, 4L))
                .thenReturn(List.of(taxDetails));

        UniqueViolationException ex = assertThrows(UniqueViolationException.class,
                () -> taxDetailsService.updateTaxDetails(4L, command));
        assertEquals("Existing tax details for HU overlap with the specified period (null-null). Overlapping ids: [5]",
                ex.getMessage());
    }

    @Test
    void getTaxDetails_found() {
        TaxDetails taxDetails = new TaxDetails();
        taxDetails.setId(5L);
        taxDetails.setTaxResidence("HU");
        when(taxDetailsRepository.getForResidenceAndDate("HU", LocalDate.EPOCH))
                .thenReturn(Optional.of(taxDetails));

        TaxDetails queried = taxDetailsService.getTaxDetails("HU", LocalDate.EPOCH);
        assertEquals(5L, queried.getId());
        assertEquals("HU", queried.getTaxResidence());
    }

    @Test
    void getTaxDetails_notFound() {
        BrokerEntityNotFoundException ex = assertThrows(BrokerEntityNotFoundException.class,
                () -> taxDetailsService.getTaxDetails("HU", LocalDate.EPOCH));
        assertEquals("No tax details for HU on 1970-01-01", ex.getMessage());
    }

    @Test
    void deleteTaxDetail_success() {
        assertDoesNotThrow(() -> taxDetailsService.deleteTaxDetail(1L));
    }

    @Test
    void deleteTaxDetail_notFound() {
        doThrow(new EmptyResultDataAccessException("", 1))
                .when(taxDetailsRepository).deleteById(1L);

        BrokerEntityNotFoundException ex = assertThrows(BrokerEntityNotFoundException.class,
                () -> taxDetailsService.deleteTaxDetail(1L));
        assertEquals("No tax detail found with database id: 1", ex.getMessage());
    }
}