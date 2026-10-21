package dev.confluxlab;

import java.util.UUID;

import dev.confluxlab.capability.api.FamilyRequests;
import dev.confluxlab.capability.api.InvalidFamilyRequestException;
import dev.confluxlab.contracts.CreateFamilyRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ConfluxLabApplication.class)
@AutoConfigureMockMvc
class FamilyRequestAcceptanceFailureHttpTest extends MongoTestSupport {

    private static final String PERSONAL_MARKER = "INT08FictivePerson";
    private static final String VALID_REQUEST = """
            {
              "principal": {"firstName": "%s", "lastName": "Exemple", "birthDate": "1990-04-12"},
              "dependents": [
                {"firstName": "Camille", "lastName": "Exemple", "birthDate": "2015-08-21"},
                {"firstName": "Noa", "lastName": "Exemple", "birthDate": "2018-02-03"}
              ]
            }
            """.formatted(PERSONAL_MARKER);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FamilyRequests familyRequests;

    @Test
    void returnsAGenericProblemWhenTheFamilyRequestsTestDoubleThrowsDuringAcceptance() throws Exception {
        String internalMessage = "Simulated acceptance failure: private storage details.";
        String privateCauseMessage = "Simulated acceptance cause: private storage endpoint.";
        when(familyRequests.createFamily(any(CreateFamilyRequest.class)))
                .thenThrow(new IllegalStateException(internalMessage, new IllegalArgumentException(privateCauseMessage)));

        ResultActions result = mockMvc.perform(post("/api/v1/families")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST));

        assertGenericProblem(result, 500, "Internal Server Error", "An unexpected error occurred.",
                internalMessage, privateCauseMessage, "IllegalStateException", "IllegalArgumentException");
        verify(familyRequests).createFamily(any(CreateFamilyRequest.class));
        verifyNoMoreInteractions(familyRequests);
    }

    @Test
    void returnsAGeneric500RatherThan404WhenTheFamilyRequestsTestDoubleThrowsDuringLookup() throws Exception {
        UUID requestId = UUID.fromString("e91aa057-68f9-4d38-9d24-e8eb7f747631");
        String internalMessage = "Simulated lookup failure: private query details.";
        String privateCauseMessage = "Simulated lookup cause for " + PERSONAL_MARKER;
        when(familyRequests.getRequest(requestId))
                .thenThrow(new IllegalStateException(internalMessage, new IllegalArgumentException(privateCauseMessage)));

        ResultActions result = mockMvc.perform(get("/api/v1/requests/{requestId}", requestId));

        assertGenericProblem(result, 500, "Internal Server Error", "An unexpected error occurred.",
                internalMessage, privateCauseMessage, "IllegalStateException", "IllegalArgumentException");
        verify(familyRequests).getRequest(requestId);
        verifyNoMoreInteractions(familyRequests);
    }

    @Test
    void returnsAGeneric400WhenCapabilityRefusesAnOtherwiseValidRequest() throws Exception {
        String privateCauseMessage = "Simulated private validation cause for " + PERSONAL_MARKER;
        InvalidFamilyRequestException refusal = new InvalidFamilyRequestException();
        refusal.initCause(new IllegalArgumentException(privateCauseMessage));
        when(familyRequests.createFamily(any(CreateFamilyRequest.class))).thenThrow(refusal);

        ResultActions result = mockMvc.perform(post("/api/v1/families")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST));

        assertGenericProblem(result, 400, "Bad Request", "Invalid request.",
                refusal.getMessage(), privateCauseMessage, "InvalidFamilyRequestException", "IllegalArgumentException");
        verify(familyRequests).createFamily(any(CreateFamilyRequest.class));
        verifyNoMoreInteractions(familyRequests);
    }

    private void assertGenericProblem(
            ResultActions result, int expectedStatus, String expectedTitle, String expectedDetail, String... privateDetails)
            throws Exception {
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
        for (String privateDetail : privateDetails) {
            result.andExpect(content().string(not(containsString(privateDetail))));
        }
    }
}
