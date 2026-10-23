package dev.confluxlab.capability.internal.client;

import java.util.concurrent.TimeUnit;

import feign.Client;
import feign.Request;
import feign.Retryer;
import feign.hc5.ApacheHttp5Client;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

// Registered only by FamilyPartnerClient, never by the application's component scan.
class FamilyPartnerFeignConfiguration {

    @Bean
    Retryer familyPartnerRetryer() {
        return Retryer.NEVER_RETRY;
    }

    @Bean
    Request.Options familyPartnerOptions(
            @Value("${conflux.partner.connect-timeout-ms:1000}") int connectTimeout,
            @Value("${conflux.partner.read-timeout-ms:3000}") int readTimeout) {
        if (connectTimeout < 1 || readTimeout < 1) {
            throw new IllegalArgumentException("Partner connection and read timeouts must be positive.");
        }
        return new Request.Options(connectTimeout, TimeUnit.MILLISECONDS, readTimeout, TimeUnit.MILLISECONDS, false);
    }

    @Bean(destroyMethod = "close")
    CloseableHttpClient familyPartnerHttpClient() {
        return HttpClients.custom()
                .disableAutomaticRetries()
                .disableRedirectHandling()
                .build();
    }

    @Bean
    Client familyPartnerFeignClient(CloseableHttpClient familyPartnerHttpClient) {
        return new ApacheHttp5Client(familyPartnerHttpClient);
    }
}
