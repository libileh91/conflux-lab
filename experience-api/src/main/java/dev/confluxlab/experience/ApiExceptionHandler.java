package dev.confluxlab.experience;

import java.net.URI;

import dev.confluxlab.capability.api.InvalidFamilyRequestException;
import dev.confluxlab.capability.api.RequestNotFoundException;
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

    @ExceptionHandler(RequestNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleRequestNotFound(RequestNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem(HttpStatus.NOT_FOUND));
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

            default -> status.is5xxServerError() ? "An unexpected error occurred." : "Request rejected.";
        };
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        // A non-null empty URI-reference prevents MVC from echoing the rejected request path.
        problem.setInstance(URI.create(""));
        return problem;
    }
}
