package dev.confluxlab.experience;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(CapabilityProperties.class)
public class ExperienceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExperienceApplication.class, args);
    }
}
