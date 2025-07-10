package uk.tw.energy.infrastructure.web.exception;

import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.List;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import uk.tw.energy.infrastructure.web.model.RestApiError;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgument(IllegalArgumentException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Validation Failed", ex.getMessage(), List.of());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex) {
        List<String> details = ex.getConstraintViolations().stream()
                .map(constraintViolation -> String.format(
                        "%s: %s", constraintViolation.getPropertyPath(), constraintViolation.getMessage()))
                .toList();
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Validation Failed", ex.getMessage(), details);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Object> handleSmartMeterNotFoundException(NotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .toList();

        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Validation Failed", "Invalid request parameters", details);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGenericException(Exception ex) {
        ex.printStackTrace();
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred",
                List.of(ex.getMessage()));
    }

    private ResponseEntity<Object> buildErrorResponse(
            HttpStatus status, String error, String message, List<String> details) {
        RestApiError errorResponse = RestApiError.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(error)
                .message(message)
                .details(details)
                .build();

        return ResponseEntity.status(status).body(errorResponse.toMap());
    }
}
