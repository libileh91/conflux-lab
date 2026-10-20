package dev.confluxlab.experience;

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

    @ExceptionHandler(CapabilityException.class)
    public ResponseEntity<ProblemDetail> handleCapability(CapabilityException exception) {
        return ResponseEntity.status(exception.status()).body(problem(exception.status()));
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
            case 502 -> "Invalid response from the upstream service.";
            case 503 -> "Upstream service unavailable.";
            case 504 -> "Upstream service timed out.";
            default -> status.is5xxServerError() ? "An unexpected error occurred." : "Request rejected.";
        };
        return ProblemDetail.forStatusAndDetail(status, detail);
    }
}
