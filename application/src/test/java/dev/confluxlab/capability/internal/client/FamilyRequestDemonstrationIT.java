package dev.confluxlab.capability.internal.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import dev.confluxlab.ConfluxLabApplication;
import dev.confluxlab.MongoTestSupport;
import dev.confluxlab.capability.internal.orchestration.FamilyOperations;
import dev.confluxlab.capability.internal.persistence.MongoFamilyRequestStore;
import dev.confluxlab.capability.internal.persistence.StoredFamilyRequest;
import dev.confluxlab.contracts.CreateFamilyRequest;
import dev.confluxlab.contracts.PersonPayload;
import dev.confluxlab.contracts.RequestStatus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = ConfluxLabApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"server.address=127.0.0.1", "conflux.processing.workers=2"})
@ExtendWith(OutputCaptureExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class FamilyRequestDemonstrationIT extends MongoTestSupport {

    private static final int PARTNER_DELAY_MS = 150;
    private static final String PRIVATE_ERROR = "DEMO_PRIVATE_PARTNER_BODY";
    private static final WireMockServer PARTNER = new WireMockServer(wireMockConfig().bindAddress("127.0.0.1").dynamicPort());

    @LocalServerPort
    private int port;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private MongoFamilyRequestStore store;
    @Autowired
    private FamilyOperations operations;
    @Autowired
    @Qualifier("familyRequestExecutor")
    private ThreadPoolTaskExecutor executor;

    @DynamicPropertySource
    static void partnerProperties(DynamicPropertyRegistry registry) {
        PARTNER.start();
        registry.add("conflux.partner.base-url", PARTNER::baseUrl);
    }

    @AfterAll
    static void stopPartner() {
        PARTNER.stop();
    }

    @BeforeEach
    void resetPartner() {
        PARTNER.resetAll();
    }

    @AfterEach
    void awaitWorkersIdle() {
        await("Demonstration workers idle").atMost(Duration.ofSeconds(10))
                .until(() -> executor.getActiveCount() == 0 && executor.getThreadPoolExecutor().getQueue().isEmpty());
    }

    @Test
    void demonstratesTwoDistinctHttpRequestsWithSeparateResultsAndMeasurements(CapturedOutput output) throws Exception {
        CreateFamilyRequest successfulPayload = payload("Success");
        CreateFamilyRequest rejectedPayload = payload("Refusal");
        List<Exchange> success = exchanges(successfulPayload, "success", 201);
        List<Exchange> refusal = exchanges(rejectedPayload, "refusal", 400);
        stubFlow(success);
        stubFlow(refusal);

        try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
            long windowStart = System.nanoTime();
            CompletableFuture<TimedResponse> first = postFamily(client, successfulPayload);
            CompletableFuture<TimedResponse> second = postFamily(client, rejectedPayload);
            Receipt accepted = receipt(first.get(10, TimeUnit.SECONDS));
            Receipt rejected = receipt(second.get(10, TimeUnit.SECONDS));
            assertNotEquals(accepted.requestId(), rejected.requestId());
            awaitTracking(client, accepted, "SUCCEEDED");
            awaitTracking(client, rejected, "FAILED");
            double windowMillis = (System.nanoTime() - windowStart) / 1_000_000.0;
            awaitWorkersIdle();

            StoredFamilyRequest succeeded = store.findById(accepted.requestId()).orElseThrow();
            StoredFamilyRequest failed = store.findById(rejected.requestId()).orElseThrow();
            assertEquals(successfulPayload, succeeded.payload());
            assertEquals(rejectedPayload, failed.payload());
            assertCalls(accepted, success, 6);
            assertCalls(rejected, refusal, 4);
            assertTrue(output.getAll().contains("requestId=" + rejected.requestId()
                    + ", operation=B, failureKind=REJECTED, httpStatus=400"));
            assertTrue(output.getAll().contains("requestId=" + accepted.requestId() + ", status=SUCCEEDED"));
            assertTrue(output.getAll().contains("requestId=" + rejected.requestId() + ", status=FAILED"));
            assertFalse(output.getAll().contains(PRIVATE_ERROR));
            printHttpMeasurement(accepted, succeeded);
            printHttpMeasurement(rejected, failed);
            List<StoredFamilyRequest> sample = List.of(succeeded, failed);
            long successes = sample.stream().filter(request -> request.status() == RequestStatus.SUCCEEDED).count();
            long failures = sample.stream().filter(request -> request.status() == RequestStatus.FAILED).count();
            long completed = successes + failures;
            System.out.printf(Locale.ROOT,
                    "DEMO_RATE submitted=%d succeeded=%d failed=%d unfinished=%d successRate=%.1f%% windowMs=%.1f%n",
                    sample.size(), successes, failures, sample.size() - completed, 100.0 * successes / completed, windowMillis);
        }
    }

    @Test
    void reportsSequentialAndConcurrentGraphSamplesWithoutPerformanceAssertions() throws Exception {
        CreateFamilyRequest payload = payload("Measure");
        stubFlow(exchanges(payload, "measure", 201));
        List<FamilyGraphMeasurements.Sample> samples = FamilyGraphMeasurements.measure(operations, executor, payload);
        assertEquals(48, PARTNER.getAllServeEvents().size(), "Eight complete graphs, including two warmups.");
        System.out.printf("DEMO_ENV java=%s os=%s arch=%s processors=%d workers=2 delayMs=%d%n",
                System.getProperty("java.version"), System.getProperty("os.name"), System.getProperty("os.arch"),
                Runtime.getRuntime().availableProcessors(), PARTNER_DELAY_MS);
        StringBuilder csv = new StringBuilder("mode,run,millis\n");
        for (FamilyGraphMeasurements.Sample sample : samples) {
            String row = String.format(Locale.ROOT, "%s,%d,%.3f", sample.mode(), sample.run(), sample.millis());
            csv.append(row).append('\n');
            System.out.println("DEMO_GRAPH " + row);
        }
        Path report = Path.of("target", "failsafe-reports", "family-graph-measurements.csv");
        Files.createDirectories(report.getParent());
        Files.writeString(report, csv);
    }

    private CompletableFuture<TimedResponse> postFamily(HttpClient client, CreateFamilyRequest payload) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(origin().resolve("/api/v1/families"))
                .timeout(Duration.ofSeconds(5)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload))).build();
        long started = System.nanoTime();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> new TimedResponse(response, (System.nanoTime() - started) / 1_000_000.0));
    }

    private Receipt receipt(TimedResponse result) throws Exception {
        assertEquals(202, result.response().statusCode());
        JsonNode body = mapper.readTree(result.response().body());
        UUID requestId = UUID.fromString(body.path("requestId").asText());
        String location = result.response().headers().firstValue("Location").orElseThrow();
        assertEquals("/api/v1/requests/" + requestId, location);
        assertEquals(location, body.path("statusUrl").asText());
        assertEquals("PENDING", body.path("status").asText());
        System.out.printf("DEMO_ACCEPT requestId=%s http=202 receipt=PENDING%n", requestId);
        return new Receipt(requestId, location, result.millis());
    }

    private void awaitTracking(HttpClient client, Receipt receipt, String expected) {
        await("HTTP tracking to reach " + expected).atMost(Duration.ofSeconds(10)).until(() -> {
            HttpResponse<String> response = client.send(HttpRequest.newBuilder(origin().resolve(receipt.location()))
                    .timeout(Duration.ofSeconds(5)).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            JsonNode body = mapper.readTree(response.body());
            assertEquals(receipt.requestId().toString(), body.path("requestId").asText());
            assertEquals(3, body.size());
            return expected.equals(body.path("status").asText());
        });
    }

    private void printHttpMeasurement(Receipt receipt, StoredFamilyRequest stored) {
        double businessMillis = Duration.between(stored.createdAt(), stored.completedAt()).toMillis();
        System.out.printf(Locale.ROOT, "DEMO_HTTP requestId=%s client202Ms=%.3f businessMs=%.1f status=%s%n",
                receipt.requestId(), receipt.millis(), businessMillis, stored.status());
    }

    private void stubFlow(List<Exchange> exchanges) throws Exception {
        for (Exchange exchange : exchanges) {
            String body = exchange.status() >= 400 ? mapper.writeValueAsString(Map.of("error", PRIVATE_ERROR))
                    : exchange.id() == null ? "" : mapper.writeValueAsString(Map.of("id", exchange.id()));
            PARTNER.stubFor(post(urlEqualTo(exchange.path())).withRequestBody(equalToJson(mapper.writeValueAsString(exchange.body())))
                    .willReturn(aResponse().withStatus(exchange.status()).withHeader("Content-Type", "application/json")
                            .withBody(body).withFixedDelay(PARTNER_DELAY_MS)));
        }
    }

    private void assertCalls(Receipt receipt, List<Exchange> exchanges, int count) throws Exception {
        assertEquals(count, PARTNER.findAll(anyRequestedFor(anyUrl())
                .withHeader("X-Request-Id", equalTo(receipt.requestId().toString()))).size());
        for (Exchange exchange : exchanges.subList(0, count)) {
            PARTNER.verify(1, postRequestedFor(urlEqualTo(exchange.path()))
                    .withHeader("X-Request-Id", equalTo(receipt.requestId().toString()))
                    .withRequestBody(equalToJson(mapper.writeValueAsString(exchange.body()))));
        }
    }

    private URI origin() {
        return URI.create("http://127.0.0.1:" + port);
    }

    private static CreateFamilyRequest payload(String suffix) {
        return new CreateFamilyRequest(new PersonPayload("Alex" + suffix, "Exemple", LocalDate.of(1990, 4, 12)),
                List.of(new PersonPayload("Camille" + suffix, "Exemple", LocalDate.of(2015, 8, 21)),
                        new PersonPayload("Noa" + suffix, "Exemple", LocalDate.of(2018, 2, 3))));
    }

    private static List<Exchange> exchanges(CreateFamilyRequest payload, String key, int contractStatus) {
        String principal = key + "-principal";
        String contract = key + "-contract";
        return List.of(
                new Exchange("/principals", payload.principal(), 201, principal),
                new Exchange("/contracts", Map.of("principalId", principal), contractStatus, contract),
                new Exchange("/dependents", Map.of("principalId", principal, "person", payload.dependents().get(0)), 201, key + "-first"),
                new Exchange("/dependents", Map.of("principalId", principal, "person", payload.dependents().get(1)), 201, key + "-second"),
                new Exchange("/contracts/" + contract + "/dependents", Map.of("dependentId", key + "-first"), 204, null),
                new Exchange("/contracts/" + contract + "/dependents", Map.of("dependentId", key + "-second"), 204, null));
    }

    private record Exchange(String path, Object body, int status, String id) {}
    private record TimedResponse(HttpResponse<String> response, double millis) {}
    private record Receipt(UUID requestId, String location, double millis) {}
}
