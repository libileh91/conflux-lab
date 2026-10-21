package dev.confluxlab;

import dev.confluxlab.capability.api.FamilyRequests;
import dev.confluxlab.experience.FamilyController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = ConfluxLabApplication.class)
class ApplicationAssemblyTest extends MongoTestSupport {

    @Autowired
    private ApplicationContext context;

    @Test
    void assemblesTheBffAndOneCapabilityImplementationInTheSameContext() {
        assertNotNull(context.getBean(FamilyController.class));
        assertEquals(1, context.getBeansOfType(FamilyRequests.class).size());
    }
}
