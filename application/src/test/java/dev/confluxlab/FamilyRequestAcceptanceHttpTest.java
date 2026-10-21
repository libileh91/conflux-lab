package dev.confluxlab;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ConfluxLabApplication.class)
@AutoConfigureMockMvc
class FamilyRequestAcceptanceHttpTest extends MongoTestSupport {

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
    void acceptsAValidRequestAsPendingWithConsistentRelativeTrackingLinks() throws Exception {
        postValidRequestAndAssertPendingAcceptance();
    }

    @Test
    void assignsDistinctRequestIdsToTwoIdenticalPosts() throws Exception {
        UUID firstRequestId = postValidRequestAndAssertPendingAcceptance();
        UUID secondRequestId = postValidRequestAndAssertPendingAcceptance();

        assertNotEquals(firstRequestId, secondRequestId);
    }

    @Test
    void rejectsAnInvalidBodyWithoutAcknowledgingARequest() throws Exception {
        mockMvc.perform(post("/api/v1/families")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Invalid request."))
                .andExpect(jsonPath("$.requestId").doesNotExist())
                .andExpect(jsonPath("$.statusUrl").doesNotExist());
    }

    private UUID postValidRequestAndAssertPendingAcceptance() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/families")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isAccepted())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        String requestIdValue = response.path("requestId").asText();
        UUID requestId = UUID.fromString(requestIdValue);
        assertEquals(requestId.toString(), requestIdValue);
        String statusUrl = "/api/v1/requests/" + requestId;
        assertEquals(statusUrl, response.path("statusUrl").asText());
        assertEquals(statusUrl, result.getResponse().getHeader("Location"));
        return requestId;
    }
}
