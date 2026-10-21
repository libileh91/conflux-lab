package dev.confluxlab.experience;

import java.net.URI;

import dev.confluxlab.capability.api.InvalidFamilyRequestException;
import dev.confluxlab.capability.api.RequestNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ApiExceptionHandlerTest {

    @Test
    void mapsAnUnknownRequestToAGenericNotFound() {
        ResponseEntity<ProblemDetail> response = new ApiExceptionHandler()
                .handleRequestNotFound(new RequestNotFoundException());

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        ProblemDetail problem = response.getBody();
        assertNotNull(problem);
        assertEquals(404, problem.getStatus());
        assertEquals("Request not found.", problem.getDetail());
        assertEquals(URI.create(""), problem.getInstance());
    }

    @Test
    void mapsACapabilityValidationRefusalToAGenericBadRequest() {
        ResponseEntity<ProblemDetail> response = new ApiExceptionHandler()
                .handleInvalidFamilyRequest(new InvalidFamilyRequestException());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ProblemDetail problem = response.getBody();
        assertNotNull(problem);
        assertEquals(400, problem.getStatus());
        assertEquals("Invalid request.", problem.getDetail());
        assertEquals(URI.create(""), problem.getInstance());
    }
}
