package dev.confluxlab.capability.internal.persistence;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import dev.confluxlab.contracts.CreateFamilyRequest;
import dev.confluxlab.contracts.PersonPayload;
import dev.confluxlab.contracts.RequestStatus;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StoredFamilyRequestTest {

    private static final Instant CREATED_AT = Instant.parse("2026-10-21T12:00:00Z");
    private static final Instant STARTED_AT = CREATED_AT.plusSeconds(1);
    private static final Instant COMPLETED_AT = CREATED_AT.plusSeconds(2);

    @Test
    void followsBothAllowedLifecyclesWithStableIdentityAndCoherentDates() {
        StoredFamilyRequest pending = pendingRequest();
        assertEquals(RequestStatus.PENDING, pending.status());
        assertEquals(CREATED_AT, pending.updatedAt());
        assertNull(pending.completedAt());

        StoredFamilyRequest processing = pending.transitionTo(RequestStatus.PROCESSING, STARTED_AT);
        assertEquals(RequestStatus.PROCESSING, processing.status());
        assertEquals(STARTED_AT, processing.updatedAt());
        assertNull(processing.completedAt());
        assertEquals(RequestStatus.PENDING, pending.status());

        for (RequestStatus terminal : List.of(RequestStatus.SUCCEEDED, RequestStatus.FAILED)) {
            StoredFamilyRequest completed = processing.transitionTo(terminal, COMPLETED_AT);
            assertEquals(terminal, completed.status());
            assertEquals(pending.requestId(), completed.requestId());
            assertEquals(pending.payload(), completed.payload());
            assertEquals(CREATED_AT, completed.createdAt());
            assertEquals(COMPLETED_AT, completed.updatedAt());
            assertEquals(COMPLETED_AT, completed.completedAt());
        }
    }

    @Test
    void rejectsSkippedStepsBackwardsDatesAndReopeningTerminalStates() {
        StoredFamilyRequest pending = pendingRequest();
        assertThrows(IllegalStateException.class,
                () -> pending.transitionTo(RequestStatus.SUCCEEDED, COMPLETED_AT));
        StoredFamilyRequest processing = pending.transitionTo(RequestStatus.PROCESSING, STARTED_AT);
        assertThrows(IllegalStateException.class,
                () -> processing.transitionTo(RequestStatus.PENDING, COMPLETED_AT));
        assertThrows(IllegalArgumentException.class,
                () -> processing.transitionTo(RequestStatus.SUCCEEDED, CREATED_AT));

        for (RequestStatus terminal : List.of(RequestStatus.SUCCEEDED, RequestStatus.FAILED)) {
            StoredFamilyRequest completed = processing.transitionTo(terminal, COMPLETED_AT);
            assertThrows(IllegalStateException.class,
                    () -> completed.transitionTo(RequestStatus.PROCESSING, COMPLETED_AT.plusSeconds(1)));
            assertEquals(terminal, completed.status());
            assertEquals(COMPLETED_AT, completed.completedAt());
        }
    }

    private static StoredFamilyRequest pendingRequest() {
        CreateFamilyRequest payload = new CreateFamilyRequest(
                new PersonPayload("Alex", "Exemple", LocalDate.of(1990, 4, 12)),
                List.of(new PersonPayload("Camille", "Exemple", LocalDate.of(2015, 8, 21)),
                        new PersonPayload("Noa", "Exemple", LocalDate.of(2018, 2, 3))));
        return StoredFamilyRequest.pending(UUID.randomUUID(), payload, CREATED_AT);
    }
}
