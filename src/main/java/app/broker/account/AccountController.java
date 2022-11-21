package app.broker.account;

import app.broker.BrokerEntityNotFoundException;
import app.broker.account.association.CreateProductAssociationCommand;
import app.broker.account.association.ProductAssociationDto;
import app.broker.account.association.UpdateProductAssociationCommand;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;

import javax.validation.Valid;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@AllArgsConstructor
@Slf4j
public class AccountController {

    private final BrokerAccountService accountService;

    // Account
    @GetMapping
    public ResponseEntity<List<BrokerAccountDto>> listAccounts() {
        return ResponseEntity.ok(accountService.listAccounts());
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<BrokerAccountDto> getAccount(@PathVariable("accountId") Long accountId) {
        return ResponseEntity.ok(accountService.getById(accountId));
    }

    @GetMapping("/{accountId}/history")
    public ResponseEntity<AccountHistoryDto> getAccountHistory(@PathVariable("accountId") Long accountId) {
        return ResponseEntity.ok(accountService.getAccountHistory(accountId));
    }

    @PostMapping
    public ResponseEntity<BrokerAccountDto> addAccount(@Valid @RequestBody CreateAccountCommand command) {
        return ResponseEntity.created(URI.create("/api/v1/accounts"))
                .body(accountService.addAccount(command));
    }

    @PutMapping("/{accountId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateAccount(@PathVariable("accountId") Long id, @Valid @RequestBody UpdateAccountCommand command) {
        accountService.updateAccount(id, command);
    }

    // Association
    @GetMapping("/{accountId}/association")
    public ResponseEntity<List<ProductAssociationDto>> listAssociations(@PathVariable("accountId") Long id) {
        return ResponseEntity.ok(accountService.listAssociations(id));
    }

    @PostMapping("/{accountId}/association")
    public ResponseEntity<ProductAssociationDto> addAssociation(
            @PathVariable("accountId") Long accountId, @Valid @RequestBody CreateProductAssociationCommand command) {
        return ResponseEntity.created(URI.create("/api/v1/accounts/" + accountId + "/association"))
                .body(accountService.addProductToAccount(accountId, command));
    }

    @PutMapping("/{accountId}/association/{associationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateAssociation(
            @PathVariable("accountId") Long accountId, @PathVariable("associationId") Long associationId,
            @Valid @RequestBody UpdateProductAssociationCommand command) {
        accountService.updateAssociation(accountId, associationId, command);
    }

    @ExceptionHandler(BrokerEntityNotFoundException.class)
    public ResponseEntity<Problem> handleEntityNotFoundException(BrokerEntityNotFoundException ex) {
        Problem problem = Problem.builder()
                .withType(URI.create("account-entity/not-found"))
                .withTitle("Not found")
                .withStatus(Status.NOT_FOUND)
                .withDetail(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Problem> handleNotReadableException(HttpMessageNotReadableException ex) {
        log.info("HttpMessage was not readable: " + ex.getHttpInputMessage());

        Problem problem = Problem.builder()
                .withType(URI.create("account-entity/not-readable"))
                .withTitle("Parsing error")
                .withStatus(Status.BAD_REQUEST)
                .withDetail(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
}
