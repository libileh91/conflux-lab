package dev.confluxlab;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ConfluxLabApplication.class)
@AutoConfigureMockMvc
class FamilyRequestTrackingHttpTest extends MongoTestSupport {

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

    @Test
    void followsThePostLocationToPendingMetadataAndKeepsCreatedAtAcrossGets() throws Exception {
        MvcResult accepted = postValidRequestAndAssertPendingAcceptance();

        JsonNode firstView = getRequestAndAssertPendingMetadata(accepted);
        JsonNode repeatedView = getRequestAndAssertPendingMetadata(accepted);

        assertEquals(firstView.path("createdAt").asText(), repeatedView.path("createdAt").asText());
    }

    @Test
    void returnsAGenericNotFoundProblemForAnUnknownUuid() throws Exception {
        assertTrackingProblem("00000000-0000-0000-0000-000000000000",
                404, "Not Found", "Request not found.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-uuid", "12345", "e91aa057-68f9-4d38-9d24-e8eb7f74763x"})
    void returnsAGenericBadRequestProblemForAMalformedUuid(String requestId) throws Exception {
        assertTrackingProblem(requestId, 400, "Bad Request", "Invalid request.");
    }

    @Test
    void correlatesEachOfTwoPostsWithItsOwnTrackingView() throws Exception {
        MvcResult firstAccepted = postValidRequestAndAssertPendingAcceptance();
        MvcResult secondAccepted = postValidRequestAndAssertPendingAcceptance();

        JsonNode firstView = getRequestAndAssertPendingMetadata(firstAccepted);
        JsonNode secondView = getRequestAndAssertPendingMetadata(secondAccepted);

        assertNotEquals(firstView.path("requestId").asText(), secondView.path("requestId").asText());
    }

    private MvcResult postValidRequestAndAssertPendingAcceptance() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/families")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isAccepted())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        UUID requestId = UUID.fromString(response.path("requestId").asText());
        String statusUrl = "/api/v1/requests/" + requestId;
        assertEquals(statusUrl, response.path("statusUrl").asText());
        assertEquals(statusUrl, result.getResponse().getHeader("Location"));
        return result;
    }

    private JsonNode getRequestAndAssertPendingMetadata(MvcResult accepted) throws Exception {
        JsonNode acceptance = objectMapper.readTree(accepted.getResponse().getContentAsString());
        String requestId = acceptance.path("requestId").asText();
        String location = accepted.getResponse().getHeader("Location");

        MvcResult result = mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.requestId").value(requestId))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.createdAt").isString())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(3, response.size(), "Tracking must expose only requestId, status and createdAt.");
        assertDoesNotThrow(() -> Instant.parse(response.path("createdAt").asText()));
        return response;
    }

    private void assertTrackingProblem(
            String requestId, int expectedStatus, String expectedTitle, String expectedDetail) throws Exception {
        mockMvc.perform(get("/api/v1/requests/{requestId}", requestId))
                .andExpect(status().is(expectedStatus))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value(expectedTitle))
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andExpect(jsonPath("$.detail").value(expectedDetail))
                .andExpect(jsonPath("$.requestId").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.statusUrl").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.createdAt").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.payload").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.principal").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.dependents").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.result").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.error").doesNotHaveJsonPath());
    }
}
