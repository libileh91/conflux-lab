package dev.confluxlab.experience;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import dev.confluxlab.capability.api.FamilyRequests;
import dev.confluxlab.contracts.CreateFamilyRequest;
import dev.confluxlab.contracts.PersonPayload;
import dev.confluxlab.contracts.RequestAccepted;
import dev.confluxlab.contracts.RequestStatus;
import dev.confluxlab.contracts.RequestView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FamilyControllerTest {

    private static final UUID REQUEST_ID = UUID.fromString("e91aa057-68f9-4d38-9d24-e8eb7f747630");

    @Mock
    private FamilyRequests familyRequests;

    @Test
    void delegatesCreationToTheJavaApiAndBuildsThePublicTrackingLink() {
        CreateFamilyRequest request = new CreateFamilyRequest(
                new PersonPayload("Alex", "Exemple", LocalDate.of(1990, 4, 12)),
                List.of(
                        new PersonPayload("Camille", "Exemple", LocalDate.of(2015, 8, 21)),
                        new PersonPayload("Noa", "Exemple", LocalDate.of(2018, 2, 3))));
        when(familyRequests.createFamily(request)).thenReturn(
                new RequestAccepted(REQUEST_ID, RequestStatus.PENDING, "/ignored-by-bff"));
        FamilyController controller = new FamilyController(familyRequests);

        ResponseEntity<RequestAccepted> response = controller.createFamily(request);

        String statusUrl = "/api/v1/requests/" + REQUEST_ID;
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertEquals(URI.create(statusUrl), response.getHeaders().getLocation());
        assertEquals(new RequestAccepted(REQUEST_ID, RequestStatus.PENDING, statusUrl), response.getBody());
        verify(familyRequests).createFamily(request);
        verifyNoMoreInteractions(familyRequests);
    }

    @Test
    void delegatesTrackingToTheJavaApi() {
        RequestView view = new RequestView(REQUEST_ID, RequestStatus.PENDING,
                Instant.parse("2026-10-21T12:00:00Z"));
        when(familyRequests.getRequest(REQUEST_ID)).thenReturn(view);
        FamilyController controller = new FamilyController(familyRequests);

        ResponseEntity<RequestView> response = controller.getRequest(REQUEST_ID);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(view, response.getBody());
        verify(familyRequests).getRequest(REQUEST_ID);
        verifyNoMoreInteractions(familyRequests);
    }
}
