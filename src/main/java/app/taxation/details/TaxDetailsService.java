package app.taxation.details;

import app.broker.BrokerEntityNotFoundException;
import app.broker.UniqueViolationException;
import lombok.AllArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Service
@AllArgsConstructor
public class TaxDetailsService {

    private final TaxDetailsRepository taxDetailsRepository;
    private final TaxDetailMapper mapper;

    public List<TaxDetailsDto> listTaxDetails() {
        return mapper.toDto(taxDetailsRepository.findAll());
    }

    public TaxDetailsDto addTaxDetails(CreateTaxDetailsCommand command) {
        TaxDetails taxDetails = new TaxDetails();
        taxDetails.loadFromCommand(command);

        List<TaxDetails> overlapping = taxDetailsRepository.findForResidenceAndDate(
                taxDetails.getTaxResidence(), taxDetails.getFromDate(), taxDetails.getToDate());
        if (overlapping.isEmpty()) {
            taxDetailsRepository.save(taxDetails);
            return mapper.toDto(taxDetails);
        }
        throw new UniqueViolationException("Existing tax details for " + command.getTaxResidence()
                + " overlap with the specified period (" + command.getFromDate() + "-"
                + command.getToDate() + "). Overlapping ids: "
                + Arrays.toString(overlapping.stream().mapToLong(TaxDetails::getId).toArray()));
    }

    @Transactional
    public void updateTaxDetails(Long taxDetailId, UpdateTaxDetailsCommand command) {
        TaxDetails taxDetails = taxDetailsRepository.findById(taxDetailId)
                .orElseThrow(() -> new BrokerEntityNotFoundException("No tax detail with id: " + taxDetailId));

        List<TaxDetails> overlapping = taxDetailsRepository.findOthersForResidenceAndDate(
                command.getTaxResidence(), command.getFromDate(), command.getToDate(), taxDetailId);
        if (overlapping.isEmpty()) {
            taxDetails.loadFromCommand(command);
            return;
        }
        throw new UniqueViolationException("Existing tax details for " + command.getTaxResidence()
                + " overlap with the specified period (" + command.getFromDate() + "-"
                + command.getToDate() + "). Overlapping ids: "
                + Arrays.toString(overlapping.stream().mapToLong(TaxDetails::getId).toArray()));
    }

    public TaxDetails getTaxDetails(String taxResidence, LocalDate asOfDate) {
        return taxDetailsRepository.getForResidenceAndDate(taxResidence, asOfDate)
                .orElseThrow(() -> new BrokerEntityNotFoundException("No tax details for "
                        + taxResidence + " on " + asOfDate));
    }

    public void deleteTaxDetail(Long taxDetailId) {
        try {
            taxDetailsRepository.deleteById(taxDetailId);
        } catch (EmptyResultDataAccessException e) {
            throw new BrokerEntityNotFoundException("No tax detail found with database id: " + taxDetailId);
        }
    }
}
