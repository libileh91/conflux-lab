package dev.confluxlab.capability.internal;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.confluxlab.ConfluxLabApplication;
import dev.confluxlab.MongoTestSupport;
import dev.confluxlab.contracts.RequestStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ConfluxLabApplication.class, properties = "spring.task.execution.pool.core-size=2")
@AutoConfigureMockMvc
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
    @Qualifier("applicationTaskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;

    @MockitoSpyBean
    private MongoFamilyRequestStore store;

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
            MvcResult response = mockMvc.perform(post("/api/v1/families")
                            .contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andReturn();
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
            assertEquals(RequestStatus.PROCESSING, store.findById(requestId).orElseThrow().status());
            mockMvc.perform(get("/api/v1/requests/{requestId}", requestId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PROCESSING"));
        } finally {
            release.countDown();
            await("Async test workers to finish").atMost(Duration.ofSeconds(10))
                    .until(() -> taskExecutor.getActiveCount() == 0 && taskExecutor.getThreadPoolExecutor().getQueue().isEmpty());
        }
    }
}
