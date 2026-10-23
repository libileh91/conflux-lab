package dev.confluxlab;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FamilyRequestPersistenceIT extends MongoTestSupport {

    private static final Pattern STARTED_PORT = Pattern.compile("Tomcat started on port (\\d+)");
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(5);
    private static final String VALID_REQUEST = """
            {
              "principal": {"firstName": "Alex", "lastName": "Exemple", "birthDate": "1990-04-12"},
              "dependents": [
                {"firstName": "Camille", "lastName": "Exemple", "birthDate": "2015-08-21"},
                {"firstName": "Noa", "lastName": "Exemple", "birthDate": "2018-02-03"}
              ]
            }
            """;

    @Test
    void retainsTheSameRequestAfterRestartingThePackagedApplication() throws Exception {
        Path jar = Path.of(Objects.requireNonNull(System.getProperty("application.jar"),
                "Run this integration test through Maven verify so application.jar is configured."));
        assertTrue(Files.isRegularFile(jar), "The packaged Boot JAR must exist before integration tests.");
        Path reports = jar.getParent().resolve("failsafe-reports");
        Files.createDirectories(reports);
        ObjectMapper mapper = new ObjectMapper();
        String location;
        JsonNode beforeRestart;

        WireMockServer partner = new WireMockServer(options().bindAddress("127.0.0.1").dynamicPort());
        try (HttpClient client = HttpClient.newBuilder().connectTimeout(HTTP_TIMEOUT).build()) {
            partner.start();
            partner.stubFor(post(urlEqualTo("/principals")).willReturn(aResponse().withStatus(503)));
            RunningApplication first = start(jar, reports.resolve("persistence-before-restart.log"), partner.baseUrl());
            try (first) {
                HttpResponse<String> accepted = client.send(HttpRequest.newBuilder(first.origin().resolve("/api/v1/families"))
                                .timeout(HTTP_TIMEOUT)
                                .header("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.ofString(VALID_REQUEST))
                                .build(), HttpResponse.BodyHandlers.ofString());
                assertEquals(201, accepted.statusCode(), accepted.body());
                JsonNode receipt = mapper.readTree(accepted.body());
                UUID requestId = UUID.fromString(receipt.path("requestId").asText());
                location = accepted.headers().firstValue("Location").orElseThrow();
                assertEquals("/api/v1/requests/" + requestId, location);
                assertEquals(location, receipt.path("statusUrl").asText());
                assertEquals("PENDING", receipt.path("status").asText());

                beforeRestart = await("Request to reach FAILED before restart")
                        .atMost(Duration.ofSeconds(10))
                        .until(() -> {
                            HttpResponse<String> lookup = get(client, first.origin().resolve(location));
                            assertEquals(200, lookup.statusCode(), lookup.body());
                            return mapper.readTree(lookup.body());
                        }, view -> "FAILED".equals(view.path("status").asText()));
                assertEquals(3, beforeRestart.size(), "Tracking must contain only its three public metadata fields.");
                assertEquals(requestId.toString(), beforeRestart.path("requestId").asText());
                assertEquals("FAILED", beforeRestart.path("status").asText());
                Instant.parse(beforeRestart.path("createdAt").asText());
            }
            assertFalse(first.process().isAlive(), "The first JVM must stop before the second one starts.");

            RunningApplication second = start(jar, reports.resolve("persistence-after-restart.log"), partner.baseUrl());
            try (second) {
                assertNotEquals(first.process().pid(), second.process().pid(), "The restart must use a new JVM.");
                HttpResponse<String> lookup = get(client, second.origin().resolve(location));
                assertEquals(200, lookup.statusCode(), lookup.body());
                assertEquals(beforeRestart, mapper.readTree(lookup.body()),
                        "UUID, FAILED and original createdAt must survive the application restart.");
            }
            assertFalse(second.process().isAlive(), "The second JVM must also be stopped after the proof.");
            partner.verify(1, postRequestedFor(urlEqualTo("/principals")));
            System.out.printf("Restart persistence proof: firstPid=%d, secondPid=%d, requestId=%s, status=FAILED, both JVMs stopped.%n",
                    first.process().pid(), second.process().pid(), beforeRestart.path("requestId").asText());
        } finally {
            partner.stop();
        }
    }

    private static HttpResponse<String> get(HttpClient client, URI uri) throws Exception {
        return client.send(HttpRequest.newBuilder(uri).timeout(HTTP_TIMEOUT).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static RunningApplication start(Path jar, Path log, String partnerBaseUrl) throws Exception {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        Process process = new ProcessBuilder(java, "-jar", jar.toString(),
                "--server.address=127.0.0.1", "--server.port=0", "--spring.data.mongodb.uri=" + mongoUri(),
                "--conflux.partner.base-url=" + partnerBaseUrl)
                .redirectErrorStream(true)
                .redirectOutput(log.toFile())
                .start();
        try {
            long deadline = System.nanoTime() + Duration.ofSeconds(40).toNanos();
            while (System.nanoTime() < deadline) {
                if (!process.isAlive()) {
                    throw new IllegalStateException("Application exited with code " + process.exitValue() + "; inspect " + log);
                }
                Matcher matcher = STARTED_PORT.matcher(Files.readString(log));
                if (matcher.find()) {
                    return new RunningApplication(process, URI.create("http://127.0.0.1:" + matcher.group(1)));
                }
                // Poll a readiness signal with a deadline, rather than assuming startup takes a fixed duration.
                Thread.sleep(100);
            }
            throw new IllegalStateException("Application did not start within 40 seconds; inspect " + log);
        } catch (Exception | Error failure) {
            process.destroyForcibly();
            try {
                process.waitFor(5, TimeUnit.SECONDS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                failure.addSuppressed(interrupted);
            }
            throw failure;
        }
    }

    private record RunningApplication(Process process, URI origin) implements AutoCloseable {

        @Override
        public void close() throws InterruptedException {
            process.destroy();
            try {
                if (!process.waitFor(10, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    assertTrue(process.waitFor(5, TimeUnit.SECONDS), "Application process must stop.");
                }
            } finally {
                if (process.isAlive()) {
                    process.destroyForcibly();
                }
            }
        }
    }
}
