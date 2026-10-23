package dev.confluxlab.capability.internal.event;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import dev.confluxlab.capability.internal.orchestration.FamilyOrchestrator;
import dev.confluxlab.capability.internal.persistence.MongoFamilyRequestStore;
import dev.confluxlab.capability.internal.persistence.StoredFamilyRequest;
import dev.confluxlab.contracts.CreateFamilyRequest;
import dev.confluxlab.contracts.PersonPayload;
import dev.confluxlab.contracts.RequestStatus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class FamilyRequestListenerTest {

    @Test
    void reportsUnconfirmedSuccessWithoutLeakingPrivateDetailsOrAttemptingFailed(CapturedOutput output) {
        MongoFamilyRequestStore store = mock(MongoFamilyRequestStore.class);
        FamilyOrchestrator orchestrator = mock(FamilyOrchestrator.class);
        Instant now = Instant.parse("2026-10-22T12:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        UUID requestId = UUID.randomUUID();
        CreateFamilyRequest payload = new CreateFamilyRequest(
                new PersonPayload("Alex", "Exemple", LocalDate.of(1990, 4, 12)),
                List.of(new PersonPayload("Camille", "Exemple", LocalDate.of(2015, 8, 21)),
                        new PersonPayload("Noa", "Exemple", LocalDate.of(2018, 2, 3))));
        StoredFamilyRequest pending = StoredFamilyRequest.pending(requestId, payload, now);
        StoredFamilyRequest processing = pending.transitionTo(RequestStatus.PROCESSING, now);
        when(store.findById(requestId)).thenReturn(Optional.of(pending));
        when(store.transition(requestId, RequestStatus.PENDING, RequestStatus.PROCESSING, now))
                .thenReturn(Optional.of(processing));
        when(orchestrator.execute(requestId, processing.payload()))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(store.transition(requestId, RequestStatus.PROCESSING, RequestStatus.SUCCEEDED, now))
                .thenThrow(new DataAccessResourceFailureException("PRIVATE_MARKER"));
        FamilyRequestListener listener = new FamilyRequestListener(store, clock, orchestrator);

        assertDoesNotThrow(() -> listener.onFamilyRequestSubmitted(new FamilyRequestSubmitted(requestId)));

        // A discarded exceptional whenComplete future must not silently hide the failed write.
        assertThat(output.getAll())
                .contains("Family finalization unconfirmed: requestId=" + requestId
                        + ", targetStatus=SUCCEEDED, exceptionType=DataAccessResourceFailureException")
                .doesNotContain("PRIVATE_MARKER", "\tat ", "Family processing failed:");
        verify(store).findById(requestId);
        verify(store).transition(requestId, RequestStatus.PENDING, RequestStatus.PROCESSING, now);
        verify(orchestrator).execute(requestId, processing.payload());
        verify(store).transition(requestId, RequestStatus.PROCESSING, RequestStatus.SUCCEEDED, now);
        verify(store, never()).transition(requestId, RequestStatus.PROCESSING, RequestStatus.FAILED, now);
        verifyNoMoreInteractions(store, orchestrator);
    }
}
