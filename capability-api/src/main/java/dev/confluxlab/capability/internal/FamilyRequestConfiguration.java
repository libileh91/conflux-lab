package dev.confluxlab.capability.internal;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class FamilyRequestConfiguration {

    @Bean
    Clock familyRequestClock() {
        return Clock.systemUTC();
    }
}
