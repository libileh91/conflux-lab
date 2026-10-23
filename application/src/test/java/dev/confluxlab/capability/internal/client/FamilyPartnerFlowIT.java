package dev.confluxlab.capability.internal.client;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import dev.confluxlab.ConfluxLabApplication;
import dev.confluxlab.MongoTestSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import feign.Request;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.cloud.openfeign.FeignClientFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ConfluxLabApplication.class, properties = {
        "conflux.partner.connect-timeout-ms=1000", "conflux.partner.read-timeout-ms=3000"})
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class FamilyPartnerFlowIT extends MongoTestSupport {

    private static final String PRINCIPAL = """
            {"firstName":"Alex","lastName":"Exemple","birthDate":"1990-04-12"}
            """;
    private static final String FIRST_DEPENDENT = """
            {"firstName":"Camille","lastName":"Exemple","birthDate":"2015-08-21"}
            """;
    private static final String SECOND_DEPENDENT = """
            {"firstName":"Noa","lastName":"Exemple","birthDate":"2018-02-03"}
            """;
    private static final String VALID_REQUEST = """
            {"principal":%s,"dependents":[%s,%s]}
            """.formatted(PRINCIPAL, FIRST_DEPENDENT, SECOND_DEPENDENT);
    private static final String CONTRACT_BODY = """
            {"principalId":"principal-1"}
            """;
    private static final String FIRST_DEPENDENT_BODY = dependentBody(FIRST_DEPENDENT);
    private static final String SECOND_DEPENDENT_BODY = dependentBody(SECOND_DEPENDENT);
    private static final String FIRST_ATTACHMENT_BODY = """
            {"dependentId":"dependent-1"}
            """;
    private static final String SECOND_ATTACHMENT_BODY = """
            {"dependentId":"dependent-2"}
            """;
    private static final String PRIVATE_MARKER = "INT21_22_PRIVATE_PARTNER_ERROR_BODY";
    private static final String JSON_CONTENT_TYPE = "application/json(?:\\s*;.*)?";
    private static final String UUID_PATTERN = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
    private static final WireMockServer PARTNER = new WireMockServer(wireMockConfig().dynamicPort());

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FeignClientFactory feignClients;

    @Autowired
    @Qualifier("familyRequestExecutor")
    private ThreadPoolTaskExecutor taskExecutor;

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
    void stubPrincipalAndDependents() {
        PARTNER.resetAll();
        PARTNER.stubFor(partnerPost("/principals", PRINCIPAL)
                .willReturn(aResponse().withStatus(201).withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"principal-1\"}")));
        PARTNER.stubFor(partnerPost("/dependents", FIRST_DEPENDENT_BODY)
                .willReturn(aResponse().withStatus(201).withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"dependent-1\"}")));
        PARTNER.stubFor(partnerPost("/dependents", SECOND_DEPENDENT_BODY)
                .willReturn(aResponse().withStatus(201).withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"dependent-2\"}")));
    }

    @AfterEach
    void awaitWorkersIdle() {
        await("Family partner flow workers to finish").atMost(Duration.ofSeconds(10))
                .until(() -> taskExecutor.getActiveCount() == 0
                        && taskExecutor.getThreadPoolExecutor().getQueue().isEmpty());
    }

    @Test
    void acceptsCreatesTheFamilyThroughSixCorrelatedCallsAndExposesSucceededMetadata() throws Exception {
        Request.Options options = feignClients.getInstance("family-partner", Request.Options.class);
        assertNotNull(options, "The family-partner client must have explicit request options.");
        assertAll("Per-client Feign options",
                () -> assertEquals(1000, options.connectTimeoutMillis()),
                () -> assertEquals(3000, options.readTimeoutMillis()),
                () -> assertFalse(options.isFollowRedirects()));

        PARTNER.stubFor(partnerPost("/contracts", CONTRACT_BODY)
                .willReturn(aResponse().withStatus(201).withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"contract-1\"}")));
        PARTNER.stubFor(partnerPost("/contracts/contract-1/dependents", FIRST_ATTACHMENT_BODY)
                .willReturn(aResponse().withStatus(204)));
        PARTNER.stubFor(partnerPost("/contracts/contract-1/dependents", SECOND_ATTACHMENT_BODY)
                .willReturn(aResponse().withStatus(204)));

        Receipt receipt = postFamily();
        awaitTracking(receipt, "SUCCEEDED");

        assertEquals(6, PARTNER.getAllServeEvents().size(), "Exactly the six partner calls must be sent.");
        PARTNER.verify(1, partnerRequest("/principals", PRINCIPAL, receipt.requestId()));
        PARTNER.verify(1, partnerRequest("/contracts", CONTRACT_BODY, receipt.requestId()));
        PARTNER.verify(1, partnerRequest("/dependents", FIRST_DEPENDENT_BODY, receipt.requestId()));
        PARTNER.verify(1, partnerRequest("/dependents", SECOND_DEPENDENT_BODY, receipt.requestId()));
        PARTNER.verify(1, partnerRequest("/contracts/contract-1/dependents", FIRST_ATTACHMENT_BODY, receipt.requestId()));
        PARTNER.verify(1, partnerRequest("/contracts/contract-1/dependents", SECOND_ATTACHMENT_BODY, receipt.requestId()));
    }

    @ParameterizedTest
    @CsvSource({"400, REJECTED", "503, TRANSIENT_HTTP_ERROR"})
    void exposesFailedAfterOneContractErrorWithoutAttachmentsOrPrivateErrorInTrackingAndLogs(
            int httpStatus, String failureKind, CapturedOutput output) throws Exception {
        PARTNER.stubFor(partnerPost("/contracts", CONTRACT_BODY)
                .willReturn(aResponse().withStatus(httpStatus).withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"" + PRIVATE_MARKER + "\"}")));

        Receipt receipt = postFamily();
        JsonNode tracking = awaitTracking(receipt, "FAILED");
        awaitWorkersIdle();

        assertEquals(4, PARTNER.getAllServeEvents().size(), "Only principal, contract and both dependents are called.");
        PARTNER.verify(1, postRequestedFor(urlEqualTo("/contracts")));
        PARTNER.verify(1, partnerRequest("/principals", PRINCIPAL, receipt.requestId()));
        PARTNER.verify(1, partnerRequest("/contracts", CONTRACT_BODY, receipt.requestId()));
        PARTNER.verify(1, partnerRequest("/dependents", FIRST_DEPENDENT_BODY, receipt.requestId()));
        PARTNER.verify(1, partnerRequest("/dependents", SECOND_DEPENDENT_BODY, receipt.requestId()));
        PARTNER.verify(0, anyRequestedFor(urlPathMatching("/contracts/[^/]+/dependents")));
        assertFalse(tracking.toString().contains(PRIVATE_MARKER), "Tracking must not expose the partner error body.");
        assertFalse(output.getAll().contains(PRIVATE_MARKER), "Logs must not expose the partner error body.");
        assertTrue(output.getAll().contains("requestId=" + receipt.requestId()
                + ", operation=B, failureKind=" + failureKind + ", httpStatus=" + httpStatus));
    }

    @Test
    void aCreationReadTimeoutEndsInFailedWithoutRetryOrDownstreamCalls(CapturedOutput output) throws Exception {
        PARTNER.stubFor(partnerPost("/principals", PRINCIPAL)
                .willReturn(aResponse().withStatus(201).withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"principal-1\"}").withFixedDelay(6000)));

        Receipt receipt = postFamily();
        awaitTracking(receipt, "FAILED");
        awaitWorkersIdle();

        // WireMock may journal the delayed response after the client has already timed out.
        await("Delayed creation to be journaled").atMost(Duration.ofSeconds(10))
                .until(() -> PARTNER.getAllServeEvents().size() == 1);
        PARTNER.verify(1, partnerRequest("/principals", PRINCIPAL, receipt.requestId()));
        PARTNER.verify(0, anyRequestedFor(urlEqualTo("/contracts")));
        PARTNER.verify(0, anyRequestedFor(urlEqualTo("/dependents")));
        PARTNER.verify(0, anyRequestedFor(urlPathMatching("/contracts/[^/]+/dependents")));
        assertTrue(output.getAll().contains("requestId=" + receipt.requestId()
                + ", operation=A, failureKind=TIMEOUT, httpStatus=-1"));
    }

    private Receipt postFamily() throws Exception {
        MvcResult accepted = mockMvc.perform(post("/api/v1/families")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isAccepted())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();
        JsonNode response = objectMapper.readTree(accepted.getResponse().getContentAsString());
        UUID requestId = UUID.fromString(response.path("requestId").asText());
        String location = accepted.getResponse().getHeader("Location");
        assertEquals("/api/v1/requests/" + requestId, location);
        assertEquals(location, response.path("statusUrl").asText());
        return new Receipt(requestId, location);
    }

    private JsonNode awaitTracking(Receipt receipt, String expectedStatus) {
        // The first GET may already be terminal: no intermediate status is required.
        return await("Public tracking to reach " + expectedStatus).atMost(Duration.ofSeconds(10))
                .until(() -> {
                    MvcResult result = mockMvc.perform(get(receipt.location()))
                            .andExpect(status().isOk())
                            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                            .andExpect(jsonPath("$.requestId").value(receipt.requestId().toString()))
                            .andExpect(jsonPath("$.createdAt").isString())
                            .andReturn();
                    JsonNode view = objectMapper.readTree(result.getResponse().getContentAsString());
                    assertEquals(3, view.size(), "Tracking exposes only requestId, status and createdAt.");
                    Instant.parse(view.path("createdAt").asText());
                    return view;
                }, view -> expectedStatus.equals(view.path("status").asText()));
    }

    private static MappingBuilder partnerPost(String path, String body) {
        return WireMock.post(urlEqualTo(path))
                .withHeader("Content-Type", matching(JSON_CONTENT_TYPE))
                .withHeader("X-Request-Id", matching(UUID_PATTERN))
                .withRequestBody(equalToJson(body));
    }

    private static RequestPatternBuilder partnerRequest(String path, String body, UUID requestId) {
        return postRequestedFor(urlEqualTo(path))
                .withHeader("Content-Type", matching(JSON_CONTENT_TYPE))
                .withHeader("X-Request-Id", equalTo(requestId.toString()))
                .withRequestBody(equalToJson(body));
    }

    private static String dependentBody(String person) {
        return """
                {"principalId":"principal-1","person":%s}
                """.formatted(person);
    }

    private record Receipt(UUID requestId, String location) {}
}
