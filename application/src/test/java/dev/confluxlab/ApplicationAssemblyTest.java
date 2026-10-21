package dev.confluxlab;

import dev.confluxlab.capability.api.FamilyRequests;
import dev.confluxlab.experience.FamilyController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ConfluxLabApplication.class)
@AutoConfigureMockMvc
class ApplicationAssemblyTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void assemblesTheBffAndOneCapabilityImplementationInTheSameContext() {
        assertNotNull(context.getBean(FamilyController.class));
        assertEquals(1, context.getBeansOfType(FamilyRequests.class).size());
    }

    @Test
    void doesNotAcknowledgeARequestBeforeAcceptanceIsImplemented() throws Exception {
        mockMvc.perform(post("/api/v1/families")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "principal": {"firstName": "Alex", "lastName": "Exemple", "birthDate": "1990-04-12"},
                                  "dependents": [
                                    {"firstName": "Camille", "lastName": "Exemple", "birthDate": "2015-08-21"},
                                    {"firstName": "Noa", "lastName": "Exemple", "birthDate": "2018-02-03"}
                                  ]
                                }
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.detail").value("Family request operations are not available yet."))
                .andExpect(jsonPath("$.requestId").doesNotExist());
    }

    @Test
    void doesNotInventTrackingBeforeItIsImplemented() throws Exception {
        mockMvc.perform(get("/api/v1/requests/e91aa057-68f9-4d38-9d24-e8eb7f747630"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.detail").value("Family request operations are not available yet."))
                .andExpect(jsonPath("$.requestId").doesNotExist());
    }
}
