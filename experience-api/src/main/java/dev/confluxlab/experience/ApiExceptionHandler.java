package dev.confluxlab.experience;

import dev.confluxlab.capability.api.CapabilityNotReadyException;
import dev.confluxlab.capability.api.InvalidFamilyRequestException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(CapabilityNotReadyException.class)
    public ResponseEntity<ProblemDetail> handleCapabilityNotReady(CapabilityNotReadyException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(problem(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @ExceptionHandler(InvalidFamilyRequestException.class)
    public ResponseEntity<ProblemDetail> handleInvalidFamilyRequest(InvalidFamilyRequestException exception) {
        return ResponseEntity.badRequest().body(problem(HttpStatus.BAD_REQUEST));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception exception) {
        return ResponseEntity.internalServerError().body(problem(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception, Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        // Discard binding errors, rejected values and parser diagnostics from framework responses.
        return super.handleExceptionInternal(exception, problem(status), headers, status, request);
    }

    private static ProblemDetail problem(HttpStatusCode status) {
        String detail = switch (status.value()) {
            case 400 -> "Invalid request.";
            case 404 -> "Request not found.";
            case 503 -> "Family request operations are not available yet.";
            default -> status.is5xxServerError() ? "An unexpected error occurred." : "Request rejected.";
        };
        return ProblemDetail.forStatusAndDetail(status, detail);
    }
}
