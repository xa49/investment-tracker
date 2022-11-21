package app.manager.transaction;

import app.util.InvalidDataException;
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
@RequestMapping("/api/v1/transactions")
@AllArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<List<TransactionDto>> recordTransaction(
            @Valid @RequestBody CreateTransactionCommand command) {
        return ResponseEntity.created(URI.create("transaction"))
                .body(transactionService.addTransaction(command));
    }

    @PutMapping("/{transactionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateTransaction(
            @PathVariable("transactionId") Long transactionId, @Valid @RequestBody UpdateTransactionCommand command) {
        transactionService.updateTransaction(transactionId, command);
    }

    @DeleteMapping("/{transactionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTransaction(@PathVariable("transactionId") Long transactionId) {
        transactionService.deleteTransaction(transactionId);
    }

    @ExceptionHandler(InvalidDataException.class)
    public ResponseEntity<Problem> cannotRecordTransaction(InvalidDataException e) {
        Problem problem = Problem.builder()
                .withType(URI.create("invalid-data"))
                .withStatus(Status.BAD_REQUEST)
                .withDetail("Cannot record transaction due to the following error: " + e.getMessage())
                .build();
        return ResponseEntity.badRequest().body(problem);
    }

}
