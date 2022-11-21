package app.taxation.details;

import app.broker.BrokerEntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;

import javax.validation.Valid;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tax")
@AllArgsConstructor
@Slf4j
public class TaxDetailController {

    private final TaxDetailsService taxDetailsService;

    @GetMapping
    public ResponseEntity<List<TaxDetailsDto>> listTaxDetails() {
        return ResponseEntity.ok(taxDetailsService.listTaxDetails());
    }

    @PostMapping
    public ResponseEntity<TaxDetailsDto> addTaxDetail(@Valid @RequestBody CreateTaxDetailsCommand command) {
        return ResponseEntity.created(URI.create("tax-detail"))
                .body(taxDetailsService.addTaxDetails(command));
    }

    @PutMapping("/{taxDetailId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateTaxDetail(
            @PathVariable("taxDetailId") Long taxDetailId, @Valid @RequestBody UpdateTaxDetailsCommand command) {
        taxDetailsService.updateTaxDetails(taxDetailId, command);
    }

    @DeleteMapping("/{taxDetailId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeTaxDetail(@PathVariable("taxDetailId") Long taxDetailId) {
        taxDetailsService.deleteTaxDetail(taxDetailId);
    }

    @ExceptionHandler(BrokerEntityNotFoundException.class)
    public ResponseEntity<Problem> missingData(BrokerEntityNotFoundException e) {
        Problem problem = Problem.builder()
                .withType(URI.create("missing-entity"))
                .withStatus(Status.BAD_REQUEST)
                .withDetail("Cannot process request due to the following error: " + e.getMessage())
                .build();
        return ResponseEntity.badRequest().body(problem);
    }

}
