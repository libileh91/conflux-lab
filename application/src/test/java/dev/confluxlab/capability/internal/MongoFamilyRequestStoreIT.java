package dev.confluxlab.capability.internal;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.confluxlab.ConfluxLabApplication;
import dev.confluxlab.MongoTestSupport;
import dev.confluxlab.contracts.CreateFamilyRequest;
import dev.confluxlab.contracts.PersonPayload;
import dev.confluxlab.contracts.RequestStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ConfluxLabApplication.class)
@AutoConfigureMockMvc
class MongoFamilyRequestStoreIT extends MongoTestSupport {

    private static final CreateFamilyRequest PAYLOAD = new CreateFamilyRequest(
            new PersonPayload("Alex", "Exemple", LocalDate.of(1990, 4, 12)),
            List.of(new PersonPayload("Camille", "Exemple", LocalDate.of(2015, 8, 21)),
                    new PersonPayload("Noa", "Exemple", LocalDate.of(2018, 2, 3))));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoSpyBean
    private MongoFamilyRequestStore store;

    @Test
    void acceptsOnlyAfterInsertionAndRejectsDuplicateIdsWithoutOverwritingMongoData() throws Exception {
        MvcResult response = mockMvc.perform(post("/api/v1/families")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(PAYLOAD)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();
        JsonNode accepted = objectMapper.readTree(response.getResponse().getContentAsString());
        UUID requestId = UUID.fromString(accepted.path("requestId").asText());

        StoredFamilyRequest stored = await("Request to reach PROCESSING in MongoDB")
                        .atMost(Duration.ofSeconds(10))
                        .until(() -> store.findById(requestId).orElseThrow(),
                                request -> request.status() == RequestStatus.PROCESSING);
        assertEquals(requestId, stored.requestId());
        assertEquals(PAYLOAD, stored.payload());
        assertEquals(RequestStatus.PROCESSING, stored.status());
        assertFalse(stored.updatedAt().isBefore(stored.createdAt()), "Claim time must not precede creation.");
        assertNull(stored.completedAt());

        StoredFamilyRequest collision = StoredFamilyRequest.pending(requestId, PAYLOAD, stored.createdAt());
        verify(store).save(collision);
        assertThrows(DuplicateKeyException.class, () -> store.save(collision));
        assertEquals(stored, store.findById(requestId).orElseThrow());
    }

    @Test
    void anInsertionFailureReturnsAGeneric500InsteadOfAcknowledgingAcceptance() throws Exception {
        String privateDetails = "Simulated MongoDB write failure: private-database-address";
        doThrow(new DataAccessResourceFailureException(privateDetails))
                .when(store).save(any(StoredFamilyRequest.class));

        mockMvc.perform(post("/api/v1/families")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(PAYLOAD)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred."))
                .andExpect(jsonPath("$.requestId").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.statusUrl").doesNotHaveJsonPath())
                .andExpect(content().string(not(containsString(privateDetails))));
    }
}
