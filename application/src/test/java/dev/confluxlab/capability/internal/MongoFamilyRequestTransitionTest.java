package dev.confluxlab.capability.internal;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import dev.confluxlab.ConfluxLabApplication;
import dev.confluxlab.MongoTestSupport;
import dev.confluxlab.contracts.CreateFamilyRequest;
import dev.confluxlab.contracts.PersonPayload;
import dev.confluxlab.contracts.RequestStatus;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ConfluxLabApplication.class)
@AutoConfigureMockMvc
class MongoFamilyRequestTransitionTest extends MongoTestSupport {

    private static final Instant CREATED_AT = Instant.parse("2026-10-21T12:00:00Z");
    private static final Instant STARTED_AT = CREATED_AT.plusSeconds(1);
    private static final Instant COMPLETED_AT = CREATED_AT.plusSeconds(2);
    private static final String PRIVATE_DIAGNOSTIC = "INT11_PRIVATE_STORAGE_DIAGNOSTIC";
    private static final CreateFamilyRequest PAYLOAD = new CreateFamilyRequest(
            new PersonPayload("Alex", "Exemple", LocalDate.of(1990, 4, 12)),
            List.of(new PersonPayload("Camille", "Exemple", LocalDate.of(2015, 8, 21)),
                    new PersonPayload("Noa", "Exemple", LocalDate.of(2018, 2, 3))));

    @Autowired
    private MongoFamilyRequestStore store;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void onlyOneConcurrentCallerCanClaimTheSamePendingRequest() throws Exception {
        StoredFamilyRequest pending = savePendingRequest();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Optional<StoredFamilyRequest>> first = executor.submit(() -> claim(pending.requestId(), ready, start));
            Future<Optional<StoredFamilyRequest>> second = executor.submit(() -> claim(pending.requestId(), ready, start));
            assertTrue(ready.await(5, TimeUnit.SECONDS), "Both callers must be ready before release.");
            start.countDown();
            Optional<StoredFamilyRequest> firstResult = first.get(10, TimeUnit.SECONDS);
            Optional<StoredFamilyRequest> secondResult = second.get(10, TimeUnit.SECONDS);

            assertEquals(1L, Stream.of(firstResult, secondResult).filter(Optional::isPresent).count());
            StoredFamilyRequest winner = firstResult.or(() -> secondResult).orElseThrow();
            assertEquals(pending.transitionTo(RequestStatus.PROCESSING, STARTED_AT), winner);
            assertEquals(winner, store.findById(pending.requestId()).orElseThrow());
            assertNull(winner.completedAt());
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "Test workers must stop.");
        }
    }

    @Test
    void rejectsInvalidOrStaleUpdatesAndKeepsTerminalDataAndTrackingPrivate() throws Exception {
        UUID unknownId = UUID.randomUUID();
        assertTrue(store.transition(unknownId, RequestStatus.PENDING, RequestStatus.PROCESSING, STARTED_AT).isEmpty());
        assertTrue(store.findById(unknownId).isEmpty());

        for (RequestStatus terminal : List.of(RequestStatus.SUCCEEDED, RequestStatus.FAILED)) {
            StoredFamilyRequest pending = savePendingRequest();
            UUID requestId = pending.requestId();
            assertThrows(IllegalStateException.class,
                    () -> store.transition(requestId, RequestStatus.PENDING, terminal, COMPLETED_AT));
            assertEquals(pending, store.findById(requestId).orElseThrow());

            StoredFamilyRequest processing = store.transition(requestId,
                    RequestStatus.PENDING, RequestStatus.PROCESSING, STARTED_AT).orElseThrow();
            assertThrows(IllegalStateException.class,
                    () -> store.transition(requestId, RequestStatus.PROCESSING, RequestStatus.PENDING, COMPLETED_AT));
            assertTrue(store.transition(requestId, RequestStatus.PROCESSING, terminal, CREATED_AT).isEmpty());
            assertEquals(processing, store.findById(requestId).orElseThrow());

            // An unmapped test-only diagnostic checks partial updates and HTTP projection, not an error schema.
            mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(requestId)),
                    new Update().set("internalDiagnostic", PRIVATE_DIAGNOSTIC), StoredFamilyRequest.class);
            StoredFamilyRequest completed = store.transition(requestId,
                    RequestStatus.PROCESSING, terminal, COMPLETED_AT).orElseThrow();
            assertEquals(processing.transitionTo(terminal, COMPLETED_AT), completed);
            assertEquals(COMPLETED_AT, completed.updatedAt());
            assertEquals(COMPLETED_AT, completed.completedAt());
            assertThrows(IllegalStateException.class,
                    () -> store.transition(requestId, terminal, RequestStatus.PROCESSING, COMPLETED_AT.plusSeconds(1)));
            RequestStatus otherTerminal = terminal == RequestStatus.SUCCEEDED ? RequestStatus.FAILED : RequestStatus.SUCCEEDED;
            assertTrue(store.transition(requestId, RequestStatus.PROCESSING, otherTerminal, COMPLETED_AT.plusSeconds(1)).isEmpty());
            assertTrue(store.transition(requestId, RequestStatus.PENDING, RequestStatus.PROCESSING, COMPLETED_AT.plusSeconds(1)).isEmpty());
            assertEquals(completed, store.findById(requestId).orElseThrow());
            Document raw = mongoTemplate.findById(requestId, Document.class, "family_requests");
            assertEquals(PRIVATE_DIAGNOSTIC, raw.getString("internalDiagnostic"));

            mockMvc.perform(get("/api/v1/requests/{requestId}", requestId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.requestId").value(requestId.toString()))
                    .andExpect(jsonPath("$.status").value(terminal.name()))
                    .andExpect(jsonPath("$.createdAt").value(CREATED_AT.toString()))
                    .andExpect(jsonPath("$.payload").doesNotHaveJsonPath())
                    .andExpect(jsonPath("$.internalDiagnostic").doesNotHaveJsonPath())
                    .andExpect(jsonPath("$.error").doesNotHaveJsonPath())
                    .andExpect(jsonPath("$.trace").doesNotHaveJsonPath())
                    .andExpect(content().string(not(containsString(PRIVATE_DIAGNOSTIC))));
        }
    }

    private Optional<StoredFamilyRequest> claim(UUID requestId, CountDownLatch ready, CountDownLatch start)
            throws InterruptedException {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timed out waiting for the test start signal.");
        }
        return store.transition(requestId, RequestStatus.PENDING, RequestStatus.PROCESSING, STARTED_AT);
    }

    private StoredFamilyRequest savePendingRequest() {
        StoredFamilyRequest pending = StoredFamilyRequest.pending(UUID.randomUUID(), PAYLOAD, CREATED_AT);
        store.save(pending);
        return pending;
    }
}
