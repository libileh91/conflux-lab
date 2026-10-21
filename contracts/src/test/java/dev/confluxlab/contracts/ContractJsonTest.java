package dev.confluxlab.contracts;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ContractJsonTest {

    private static final UUID REQUEST_ID = UUID.fromString("e91aa057-68f9-4d38-9d24-e8eb7f747630");
    private static final String STATUS_URL = "/api/v1/requests/" + REQUEST_ID;
    private static final Instant CREATED_AT = Instant.parse("2026-10-21T12:00:00Z");
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void createFamilyFixtureMatchesDtoInBothDirections() throws IOException {
        var expected = new CreateFamilyRequest(
                new PersonPayload("Alex", "Exemple", LocalDate.of(1990, 4, 12)),
                List.of(
                        new PersonPayload("Camille", "Exemple", LocalDate.of(2015, 8, 21)),
                        new PersonPayload("Noa", "Exemple", LocalDate.of(2018, 2, 3))));
        JsonNode fixture = readFixture("create-family.json");

        assertEquals(expected, MAPPER.treeToValue(fixture, CreateFamilyRequest.class));
        assertEquals(fixture, MAPPER.readTree(MAPPER.writeValueAsString(expected)));
        assertAll(
                () -> assertEquals("1990-04-12", fixture.path("principal").path("birthDate").textValue()),
                () -> assertEquals("2015-08-21", fixture.path("dependents").path(0).path("birthDate").textValue()),
                () -> assertEquals("2018-02-03", fixture.path("dependents").path(1).path("birthDate").textValue()));
    }

    @Test
    void requestAcceptedFixtureMatchesDtoInBothDirections() throws IOException {
        var expected = new RequestAccepted(REQUEST_ID, RequestStatus.PENDING, STATUS_URL);
        JsonNode fixture = readFixture("request-accepted.json");

        assertEquals(expected, MAPPER.treeToValue(fixture, RequestAccepted.class));
        assertEquals(fixture, MAPPER.readTree(MAPPER.writeValueAsString(expected)));
        assertAll(
                () -> assertEquals(REQUEST_ID.toString(), fixture.path("requestId").textValue()),
                () -> assertEquals("PENDING", fixture.path("status").textValue()),
                () -> assertEquals(STATUS_URL, fixture.path("statusUrl").textValue()));
    }

    @Test
    void requestViewFixtureMatchesDtoInBothDirections() throws IOException {
        var expected = new RequestView(REQUEST_ID, RequestStatus.PENDING, CREATED_AT);
        JsonNode fixture = readFixture("request-view.json");

        assertEquals(expected, MAPPER.treeToValue(fixture, RequestView.class));
        assertEquals(fixture, MAPPER.readTree(MAPPER.writeValueAsString(expected)));
        assertAll(
                () -> assertEquals(REQUEST_ID.toString(), fixture.path("requestId").textValue()),
                () -> assertEquals("PENDING", fixture.path("status").textValue()),
                () -> assertEquals("2026-10-21T12:00:00Z", fixture.path("createdAt").textValue()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"PENDING", "PROCESSING", "SUCCEEDED", "FAILED"})
    void serializesAndReadsStatusesByTheirExactEnumNames(String statusName) throws IOException {
        RequestStatus status = RequestStatus.valueOf(statusName);
        String json = "\"" + statusName + "\"";

        assertEquals(json, MAPPER.writeValueAsString(status));
        assertEquals(status, MAPPER.readValue(json, RequestStatus.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"UNKNOWN", "pending"})
    void rejectsUnknownOrIncorrectlyCasedStatuses(String status) throws IOException {
        assertInvalidField("request-accepted.json", RequestAccepted.class, "status", status);
        assertInvalidField("request-view.json", RequestView.class, "status", status);
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-date", "2026-02-30"})
    void rejectsMalformedOrImpossibleBirthDates(String birthDate) throws IOException {
        ObjectNode json = readFixture("create-family.json");
        ((ObjectNode) json.get("principal")).put("birthDate", birthDate);

        assertThrows(InvalidFormatException.class, () -> MAPPER.treeToValue(json, CreateFamilyRequest.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-an-instant", "2026-10-21"})
    void rejectsInvalidCreationInstants(String createdAt) throws IOException {
        assertInvalidField("request-view.json", RequestView.class, "createdAt", createdAt);
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-uuid", "e91aa057-68f9-4d38-9d24-e8eb7f74763x"})
    void rejectsInvalidRequestIdentifiers(String requestId) throws IOException {
        assertInvalidField("request-accepted.json", RequestAccepted.class, "requestId", requestId);
        assertInvalidField("request-view.json", RequestView.class, "requestId", requestId);
    }

    @Test
    void deserializationDoesNotEvaluateBeanValidationConstraints() throws IOException {
        var family = MAPPER.readValue("""
                {
                  "principal": null,
                  "dependents": []
                }
                """, CreateFamilyRequest.class);
        var person = MAPPER.readValue("""
                {
                  "firstName": " ",
                  "lastName": null,
                  "birthDate": "2026-10-22"
                }
                """, PersonPayload.class);
        var accepted = MAPPER.readValue("{}", RequestAccepted.class);
        var view = MAPPER.readValue("{}", RequestView.class);

        assertAll(
                () -> assertNull(family.principal()),
                () -> assertTrue(family.dependents().isEmpty()),
                () -> assertEquals(" ", person.firstName()),
                () -> assertNull(person.lastName()),
                () -> assertEquals(LocalDate.of(2026, 10, 22), person.birthDate()),
                () -> assertNull(accepted.requestId()),
                () -> assertNull(accepted.status()),
                () -> assertNull(accepted.statusUrl()),
                () -> assertNull(view.requestId()),
                () -> assertNull(view.status()),
                () -> assertNull(view.createdAt()));
    }

    private static void assertInvalidField(
            String fixtureName, Class<?> dtoType, String field, String value) throws IOException {
        ObjectNode json = readFixture(fixtureName);
        json.put(field, value);

        assertThrows(InvalidFormatException.class, () -> MAPPER.treeToValue(json, dtoType));
    }

    private static ObjectNode readFixture(String fixtureName) throws IOException {
        try (InputStream input = ContractJsonTest.class.getResourceAsStream("/contracts/" + fixtureName)) {
            assertNotNull(input, () -> "Missing fixture: /contracts/" + fixtureName);
            return (ObjectNode) MAPPER.readTree(input);
        }
    }
}
