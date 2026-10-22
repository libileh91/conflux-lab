package dev.confluxlab.capability.internal;

import java.time.Clock;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.mongodb.WriteConcern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration(proxyBeanMethods = false)
@EnableAsync
class FamilyRequestConfiguration implements AsyncConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(FamilyRequestConfiguration.class);

    @Bean
    MongoTemplate mongoTemplate(MongoDatabaseFactory factory, MongoConverter converter) {
        MongoTemplate template = new MongoTemplate(factory, converter);
        template.setWriteConcern(WriteConcern.MAJORITY.withJournal(true).withWTimeout(5, TimeUnit.SECONDS));
        return template;
    }

    @Bean
    Clock familyRequestClock() {
        return Clock.systemUTC();
    }

    @Bean
    ThreadPoolTaskExecutor familyRequestExecutor(
            @Value("${conflux.processing.workers:2}") int workers,
            @Value("${conflux.processing.queue-capacity:16}") int queueCapacity,
            @Value("${conflux.processing.shutdown-await-seconds:5}") int shutdownAwaitSeconds) {
        if (workers < 1 || queueCapacity < 0 || shutdownAwaitSeconds < 1) {
            throw new IllegalArgumentException("Processing workers and shutdown wait must be positive; queue capacity must be non-negative.");
        }
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(workers);
        executor.setMaxPoolSize(workers);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("family-request-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(shutdownAwaitSeconds);
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (failure, method, parameters) -> {
            String requestId = parameters.length > 0 && parameters[0] instanceof FamilyRequestSubmitted submitted
                    ? submitted.requestId().toString() : "unknown";
            // Exception messages and event arguments may contain personal data; do not log them.
            LOGGER.error("Async family request failed: requestId={}, exceptionType={}",
                    requestId, failure.getClass().getSimpleName());
        };
    }
}
