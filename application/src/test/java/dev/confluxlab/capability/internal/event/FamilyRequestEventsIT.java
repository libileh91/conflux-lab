package dev.confluxlab.capability.internal.event;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import dev.confluxlab.ConfluxLabApplication;
import dev.confluxlab.MongoTestSupport;
import dev.confluxlab.capability.internal.orchestration.FamilyOrchestrator;
import dev.confluxlab.capability.internal.persistence.MongoFamilyRequestStore;
import dev.confluxlab.capability.internal.persistence.StoredFamilyRequest;
import dev.confluxlab.contracts.CreateFamilyRequest;
import dev.confluxlab.contracts.RequestStatus;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ConfluxLabApplication.class, properties = {
        "conflux.processing.workers=2", "conflux.processing.queue-capacity=1"})
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class FamilyRequestEventsIT extends MongoTestSupport {

    private static final String VALID_REQUEST = """
            {
              "principal": {"firstName": "Alex", "lastName": "Exemple", "birthDate": "1990-04-12"},
              "dependents": [
                {"firstName": "Camille", "lastName": "Exemple", "birthDate": "2015-08-21"},
                {"firstName": "Noa", "lastName": "Exemple", "birthDate": "2018-02-03"}
              ]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicationEventPublisher events;

    @Autowired
    @Qualifier("familyRequestExecutor")
    private ThreadPoolTaskExecutor taskExecutor;

    @MockitoSpyBean
    private MongoFamilyRequestStore store;

    @Autowired
    private MongoTemplate mongoTemplate;

    @MockitoBean
    private FamilyOrchestrator orchestrator;

    @BeforeEach
    void keepOrchestrationIncomplete() {
        // Test-only: isolate claiming without partner HTTP calls or finalization.
        when(orchestrator.execute(any(UUID.class), any(CreateFamilyRequest.class)))
                .thenAnswer(invocation -> new CompletableFuture<>());
    }

    @Test
    void acceptsWhileTheListenerIsBlockedAndOnlyOneDuplicateEventCanClaim() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(2);
        Queue<Optional<?>> results = new ConcurrentLinkedQueue<>();
        Queue<Thread> workers = new ConcurrentLinkedQueue<>();
        Thread requestThread = Thread.currentThread();
        doAnswer(invocation -> {
            workers.add(Thread.currentThread());
            ready.countDown();
            try {
                assertTrue(release.await(10, TimeUnit.SECONDS), "Test must release both claim attempts.");
                Optional<?> result = (Optional<?>) invocation.callRealMethod();
                results.add(result);
                return result;
            } finally {
                finished.countDown();
            }
        }).when(store).transition(any(UUID.class), eq(RequestStatus.PENDING), eq(RequestStatus.PROCESSING), any(Instant.class));

        try {
            MvcResult response = postAcceptedRequest();
            UUID requestId = UUID.fromString(objectMapper.readTree(response.getResponse().getContentAsString())
                    .path("requestId").asText());
            events.publishEvent(new FamilyRequestSubmitted(requestId));

            assertTrue(ready.await(5, TimeUnit.SECONDS), "Both event deliveries must reach the claim before release.");
            assertTrue(workers.stream().allMatch(worker -> worker != requestThread), "The listener must not run on the HTTP thread.");
            assertEquals(RequestStatus.PENDING, store.findById(requestId).orElseThrow().status());

            release.countDown();
            assertTrue(finished.await(10, TimeUnit.SECONDS), "Both claim attempts must finish.");
            assertEquals(2, results.size(), "Both MongoDB attempts must return normally; inspect async logs otherwise.");
            assertEquals(1L, results.stream().filter(Optional::isPresent).count());
            awaitWorkersIdle();
            verify(orchestrator).execute(eq(requestId), any(CreateFamilyRequest.class));
            assertEquals(RequestStatus.PROCESSING, store.findById(requestId).orElseThrow().status());
            mockMvc.perform(get("/api/v1/requests/{requestId}", requestId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PROCESSING"));
        } finally {
            release.countDown();
            awaitWorkersIdle();
        }
    }

    @Test
    void rejectsTheNextRequestWhenWorkersAndQueueAreFull() throws Exception {
        assertEquals(2, taskExecutor.getCorePoolSize());
        assertEquals(2, taskExecutor.getMaxPoolSize());
        assertEquals(1, taskExecutor.getQueueCapacity());
        CountDownLatch running = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        Queue<Thread> workers = new ConcurrentLinkedQueue<>();
        Thread requestThread = Thread.currentThread();
        doAnswer(invocation -> {
            workers.add(Thread.currentThread());
            running.countDown();
            assertTrue(release.await(10, TimeUnit.SECONDS), "Test must release the saturated workers.");
            return invocation.callRealMethod();
        }).when(store).transition(any(UUID.class), eq(RequestStatus.PENDING), eq(RequestStatus.PROCESSING), any(Instant.class));

        try {
            postAcceptedRequest();
            postAcceptedRequest();
            assertTrue(running.await(5, TimeUnit.SECONDS), "Both workers must be occupied before filling the queue.");
            postAcceptedRequest();
            assertEquals(1, taskExecutor.getThreadPoolExecutor().getQueue().size());

            mockMvc.perform(post("/api/v1/families")
                            .contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                    .andExpect(status().isInternalServerError())
                    .andExpect(header().doesNotExist("Location"))
                    .andExpect(jsonPath("$.detail").value("An unexpected error occurred."))
                    .andExpect(jsonPath("$.requestId").doesNotHaveJsonPath());
            assertEquals(2, workers.size(), "The rejected task must not execute on the HTTP thread.");
            assertTrue(workers.stream().allMatch(worker -> worker != requestThread));

            ArgumentCaptor<StoredFamilyRequest> saved = ArgumentCaptor.forClass(StoredFamilyRequest.class);
            verify(store, times(4)).save(saved.capture());
            UUID rejectedId = saved.getAllValues().get(3).requestId();
            release.countDown();
            awaitWorkersIdle();
            verify(store, times(3)).transition(any(UUID.class), eq(RequestStatus.PENDING), eq(RequestStatus.PROCESSING), any(Instant.class));
            assertEquals(RequestStatus.PENDING, store.findById(rejectedId).orElseThrow().status(),
                    "Insertion is not rolled back when submission is rejected.");
        } finally {
            release.countDown();
            awaitWorkersIdle();
        }
    }

    @Test
    void logsAsyncFailureWithItsRequestIdWithoutLeakingTheExceptionMessage(CapturedOutput output) throws Exception {
        String privateMarker = "INT15_PRIVATE_PAYLOAD_MARKER";
        doThrow(new DataAccessResourceFailureException(privateMarker)).when(store).findById(any(UUID.class));

        try {
            MvcResult response = postAcceptedRequest();
            UUID requestId = UUID.fromString(objectMapper.readTree(response.getResponse().getContentAsString())
                    .path("requestId").asText());
            String expectedLog = "Async family request failed: requestId=" + requestId
                    + ", exceptionType=DataAccessResourceFailureException";
            await("Correlated async error log").atMost(Duration.ofSeconds(5))
                    .untilAsserted(() -> assertTrue(output.getAll().contains(expectedLog)));
            awaitWorkersIdle();
            assertFalse(output.getAll().contains(privateMarker));
            StoredFamilyRequest stored = mongoTemplate.findById(requestId, StoredFamilyRequest.class);
            assertEquals(RequestStatus.PENDING, stored.status(), "A failure before claiming must not invent FAILED.");
        } finally {
            awaitWorkersIdle();
        }
    }

    private MvcResult postAcceptedRequest() throws Exception {
        return mockMvc.perform(post("/api/v1/families")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();
    }

    private void awaitWorkersIdle() {
        await("Async test workers to finish").atMost(Duration.ofSeconds(10))
                .until(() -> taskExecutor.getActiveCount() == 0 && taskExecutor.getThreadPoolExecutor().getQueue().isEmpty());
    }
}
