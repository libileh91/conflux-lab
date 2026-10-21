package dev.confluxlab;

import java.time.Duration;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public abstract class MongoTestSupport {

    // One standalone MongoDB per test JVM; Testcontainers removes it when that JVM exits.
    private static final GenericContainer<?> MONGO = new GenericContainer<>(DockerImageName.parse("mongo:8.0.16"))
            .withExposedPorts(27017)
            .withCommand("mongod", "--bind_ip_all", "--wiredTigerCacheSizeGB", "0.25")
            .withStartupTimeout(Duration.ofSeconds(60));

    static {
        MONGO.start();
    }

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> "mongodb://" + MONGO.getHost() + ":"
                + MONGO.getMappedPort(27017) + "/conflux_lab_test"
                + "?serverSelectionTimeoutMS=3000&connectTimeoutMS=3000&socketTimeoutMS=5000&retryWrites=false");
    }
}
