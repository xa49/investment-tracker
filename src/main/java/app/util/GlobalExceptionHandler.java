package app.util;

import app.broker.BrokerEntityNotFoundException;
import app.broker.UniqueViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;

import java.net.URI;
import java.util.List;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UniqueViolationException.class)
    public ResponseEntity<Problem> handleNotUniqueException(UniqueViolationException ex) {
        Problem problem = Problem.builder()
                .withType(URI.create("entity/not-unique"))
                .withTitle("Not unique")
                .withStatus(Status.BAD_REQUEST)
                .withDetail(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Problem> handleValidationFailedException(MethodArgumentNotValidException ex) {
        List<Violation> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new Violation(fe.getField(), fe.getDefaultMessage()))
                .toList();
        Problem problem = Problem.builder()
                .withType(URI.create("entity/not-valid"))
                .withTitle("Validation error")
                .withStatus(Status.BAD_REQUEST)
                .with("violations", violations)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Problem> handleNotReadableException(HttpMessageNotReadableException ex) {
        log.info("HttpMessage was not readable: " + ex.getHttpInputMessage());

        Problem problem = Problem.builder()
                .withType(URI.create("broker-entity/not-readable"))
                .withTitle("Parsing error")
                .withStatus(Status.BAD_REQUEST)
                .withDetail(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }


    @ExceptionHandler(BrokerEntityNotFoundException.class)
    public ResponseEntity<Problem> handleEntityNotFoundException(BrokerEntityNotFoundException ex) {
        Problem problem = Problem.builder()
                .withType(URI.create("broker-entity/not-found"))
                .withTitle("Not found")
                .withStatus(Status.NOT_FOUND)
                .withDetail(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(MissingDataException.class)
    public ResponseEntity<Problem> missingDataException(MissingDataException ex) {
        Problem problem = Problem.builder()
                .withType(URI.create("missing-data"))
                .withTitle("Missing data")
                .withStatus(Status.BAD_REQUEST)
                .withDetail(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
}
