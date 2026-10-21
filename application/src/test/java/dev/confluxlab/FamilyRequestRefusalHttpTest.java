package dev.confluxlab;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import dev.confluxlab.capability.api.FamilyRequests;
import dev.confluxlab.contracts.CreateFamilyRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ConfluxLabApplication.class)
@AutoConfigureMockMvc
class FamilyRequestRefusalHttpTest {

    private static final String PERSONAL_MARKER = "INT08FictivePerson";
    private static final String NAME_WITH_101_CHARACTERS =
            PERSONAL_MARKER + "N".repeat(101 - PERSONAL_MARKER.length());

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoSpyBean
    private FamilyRequests familyRequests;

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("invalidRequests")
    void rejectsInvalidDataBeforeCallingCapability(String description, Consumer<ObjectNode> mutation) throws Exception {
        ObjectNode request = validRequest();
        mutation.accept(request);

        assertRejectedPost(objectMapper.writeValueAsString(request), MediaType.APPLICATION_JSON,
                400, "Bad Request", "Invalid request.");
    }

    private static Stream<Arguments> invalidRequests() {
        String futureBirthDate = LocalDate.now(ZoneOffset.UTC).plusYears(1).toString();
        return Stream.of(
                invalidRequest("principal missing", request -> request.remove("principal")),
                invalidRequest("principal null", request -> request.putNull("principal")),
                invalidRequest("dependents missing", request -> request.remove("dependents")),
                invalidRequest("dependents null", request -> request.putNull("dependents")),
                invalidRequest("no dependents", request -> dependents(request).removeAll()),
                invalidRequest("one dependent", request -> dependents(request).remove(1)),
                invalidRequest("three dependents", request ->
                        dependents(request).add(dependents(request).get(0).deepCopy())),
                invalidRequest("first dependent null", request -> dependents(request).set(0, request.nullNode())),
                invalidRequest("second dependent null", request -> dependents(request).set(1, request.nullNode())),
                invalidRequest("principal firstName missing", request -> principal(request).remove("firstName")),
                invalidRequest("principal firstName null", request -> principal(request).putNull("firstName")),
                invalidRequest("principal firstName empty", request -> principal(request).put("firstName", "")),
                invalidRequest("principal firstName blank", request -> principal(request).put("firstName", " \t\n ")),
                invalidRequest("principal firstName too long", request ->
                        principal(request).put("firstName", NAME_WITH_101_CHARACTERS)),
                invalidRequest("principal lastName missing", request -> principal(request).remove("lastName")),
                invalidRequest("principal lastName null", request -> principal(request).putNull("lastName")),
                invalidRequest("principal lastName empty", request -> principal(request).put("lastName", "")),
                invalidRequest("principal lastName blank", request -> principal(request).put("lastName", " \t\n ")),
                invalidRequest("principal lastName too long", request ->
                        principal(request).put("lastName", NAME_WITH_101_CHARACTERS)),
                invalidRequest("first dependent firstName missing", request -> dependent(request, 0).remove("firstName")),
                invalidRequest("first dependent lastName null", request -> dependent(request, 0).putNull("lastName")),
                invalidRequest("second dependent firstName blank", request ->
                        dependent(request, 1).put("firstName", " \t\n ")),
                invalidRequest("second dependent lastName too long", request ->
                        dependent(request, 1).put("lastName", NAME_WITH_101_CHARACTERS)),
                invalidRequest("principal birthDate missing", request -> principal(request).remove("birthDate")),
                invalidRequest("principal birthDate null", request -> principal(request).putNull("birthDate")),
                invalidRequest("principal birthDate in the future", request ->
                        principal(request).put("birthDate", futureBirthDate)),
                invalidRequest("first dependent birthDate missing", request -> dependent(request, 0).remove("birthDate")),
                invalidRequest("second dependent birthDate null", request -> dependent(request, 1).putNull("birthDate")),
                invalidRequest("second dependent birthDate in the future", request ->
                        dependent(request, 1).put("birthDate", futureBirthDate)),
                invalidRequest("principal birthDate malformed", request ->
                        principal(request).put("birthDate", "invalid-date-" + PERSONAL_MARKER)),
                invalidRequest("principal birthDate impossible", request ->
                        principal(request).put("birthDate", "2023-02-29")),
                invalidRequest("first dependent birthDate malformed", request ->
                        dependent(request, 0).put("birthDate", "invalid-date-" + PERSONAL_MARKER)),
                invalidRequest("second dependent birthDate impossible", request ->
                        dependent(request, 1).put("birthDate", "2024-02-30")),
                invalidRequest("principal is a string", request -> request.put("principal", PERSONAL_MARKER)),
                invalidRequest("principal is an array", request -> request.putArray("principal")),
                invalidRequest("dependents is an object", request ->
                        request.putObject("dependents").put("firstName", PERSONAL_MARKER)),
                invalidRequest("dependents is a string", request -> request.put("dependents", PERSONAL_MARKER)),
                invalidRequest("first dependent is a string", request ->
                        dependents(request).set(0, TextNode.valueOf(PERSONAL_MARKER))),
                invalidRequest("second dependent is an array", request ->
                        dependents(request).set(1, request.arrayNode())),
                invalidRequest("principal firstName is an object", request ->
                        principal(request).putObject("firstName").put("value", PERSONAL_MARKER)),
                invalidRequest("second dependent lastName is an array", request ->
                        dependent(request, 1).putArray("lastName").add(PERSONAL_MARKER)),
                invalidRequest("principal birthDate is an object", request ->
                        principal(request).putObject("birthDate").put("value", PERSONAL_MARKER)));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("unreadableBodies")
    void rejectsUnreadableBodiesBeforeCallingCapability(String description, String body) throws Exception {
        assertRejectedPost(body, MediaType.APPLICATION_JSON, 400, "Bad Request", "Invalid request.");
    }

    private static Stream<Arguments> unreadableBodies() {
        return Stream.of(
                Arguments.of("truncated JSON containing personal data",
                        "{\"principal\":{\"firstName\":\"" + PERSONAL_MARKER + "\"},\"dependents\":["),
                Arguments.of("null root", "null"),
                Arguments.of("empty body", ""),
                Arguments.of("array root", "[\"" + PERSONAL_MARKER + "\"]"),
                Arguments.of("string root", "\"" + PERSONAL_MARKER + "\""));
    }

    @Test
    void rejectsANonJsonContentTypeBeforeCallingCapability() throws Exception {
        assertRejectedPost(objectMapper.writeValueAsString(validRequest()), MediaType.TEXT_PLAIN,
                415, "Unsupported Media Type", "Request rejected.");
    }

    @Test
    void rejectsAMalformedUuidWithoutEchoingPersonalDataEvenInTheProblemInstance() throws Exception {
        ResultActions result = mockMvc.perform(get("/api/v1/requests/{requestId}", "not-a-uuid-" + PERSONAL_MARKER));

        verify(familyRequests, never()).getRequest(any());
        verify(familyRequests, never()).createFamily(any());
        assertGenericProblem(result, 400, "Bad Request", "Invalid request.");
    }

    @Test
    void acceptsNamesOfExactly100CharactersAndDelegatesToTheRealCapability() throws Exception {
        ObjectNode request = validRequest();
        String boundaryName = PERSONAL_MARKER + "N".repeat(100 - PERSONAL_MARKER.length());
        principal(request).put("firstName", boundaryName).put("lastName", boundaryName);
        for (JsonNode dependent : dependents(request)) {
            ((ObjectNode) dependent).put("firstName", boundaryName).put("lastName", boundaryName);
        }

        MvcResult result = mockMvc.perform(post("/api/v1/families")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        verify(familyRequests).createFamily(objectMapper.treeToValue(request, CreateFamilyRequest.class));
        verifyNoMoreInteractions(familyRequests);
        JsonNode acceptance = objectMapper.readTree(result.getResponse().getContentAsString());
        UUID requestId = UUID.fromString(acceptance.path("requestId").asText());
        String statusUrl = "/api/v1/requests/" + requestId;
        assertEquals(statusUrl, acceptance.path("statusUrl").asText());
        assertEquals(statusUrl, result.getResponse().getHeader("Location"));
    }

    private ObjectNode validRequest() {
        ObjectNode request = objectMapper.createObjectNode();
        request.set("principal", person("1990-04-12"));
        request.putArray("dependents").add(person("2015-08-21")).add(person("2018-02-03"));
        return request;
    }

    private ObjectNode person(String birthDate) {
        return objectMapper.createObjectNode()
                .put("firstName", PERSONAL_MARKER)
                .put("lastName", "Exemple")
                .put("birthDate", birthDate);
    }

    private static Arguments invalidRequest(String description, Consumer<ObjectNode> mutation) {
        return Arguments.of(description, mutation);
    }

    private static ObjectNode principal(ObjectNode request) {
        return (ObjectNode) request.get("principal");
    }

    private static ArrayNode dependents(ObjectNode request) {
        return (ArrayNode) request.get("dependents");
    }

    private static ObjectNode dependent(ObjectNode request, int index) {
        return (ObjectNode) dependents(request).get(index);
    }

    private void assertRejectedPost(
            String body, MediaType contentType, int expectedStatus, String expectedTitle, String expectedDetail)
            throws Exception {
        ResultActions result = mockMvc.perform(post("/api/v1/families").contentType(contentType).content(body));

        verify(familyRequests, never()).createFamily(any());
        assertGenericProblem(result, expectedStatus, expectedTitle, expectedDetail);
    }

    private void assertGenericProblem(
            ResultActions result, int expectedStatus, String expectedTitle, String expectedDetail) throws Exception {
        result.andExpect(status().is(expectedStatus))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value(expectedTitle))
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andExpect(jsonPath("$.detail").value(expectedDetail))
                .andExpect(jsonPath("$.instance").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.requestId").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.statusUrl").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.createdAt").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.payload").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.principal").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.dependents").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.result").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.error").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.errors").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.message").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.cause").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.trace").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.exception").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.stackTrace").doesNotHaveJsonPath())
                .andExpect(content().string(not(containsString(PERSONAL_MARKER))));
    }
}
