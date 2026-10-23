package dev.confluxlab.capability.internal.event;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import dev.confluxlab.capability.internal.orchestration.FamilyOrchestrator;
import dev.confluxlab.capability.internal.persistence.MongoFamilyRequestStore;
import dev.confluxlab.capability.internal.persistence.StoredFamilyRequest;
import dev.confluxlab.contracts.RequestStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
class FamilyRequestListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(FamilyRequestListener.class);

    private final MongoFamilyRequestStore store;
    private final Clock clock;
    private final FamilyOrchestrator orchestrator;

    FamilyRequestListener(MongoFamilyRequestStore store, Clock clock, FamilyOrchestrator orchestrator) {
        this.store = store;
        this.clock = clock;
        this.orchestrator = orchestrator;
    }

    @Async("familyRequestExecutor")
    @EventListener
    public void onFamilyRequestSubmitted(FamilyRequestSubmitted event) {
        store.findById(event.requestId())
                .filter(request -> request.status() == RequestStatus.PENDING)
                .flatMap(request -> store.transition(request.requestId(), RequestStatus.PENDING,
                        RequestStatus.PROCESSING, Instant.now(clock)))
                .ifPresent(request -> {
                    CompletableFuture<Void> processing;
                    try {
                        processing = orchestrator.execute(request.requestId(), request.payload());
                    } catch (RuntimeException failure) {
                        finalizeRequest(request, failure);
                        return;
                    }
                    processing.whenComplete((unused, failure) -> finalizeRequest(request, failure));
                });
    }

    private void finalizeRequest(StoredFamilyRequest request, Throwable failure) {
        RequestStatus target = failure == null ? RequestStatus.SUCCEEDED : RequestStatus.FAILED;
        if (failure != null) {
            LOGGER.error("Family processing failed: requestId={}, exceptionType={}",
                    request.requestId(), failureType(failure));
        }
        // Completion callbacks do not reach the @Async void exception handler: report write failures here.
        try {
            if (store.transition(request.requestId(), RequestStatus.PROCESSING, target, Instant.now(clock)).isEmpty()) {
                LOGGER.error("Family finalization not applied: requestId={}, targetStatus={}", request.requestId(), target);
            } else {
                LOGGER.info("Family request finalized: requestId={}, status={}", request.requestId(), target);
            }
        } catch (RuntimeException writeFailure) {
            LOGGER.error("Family finalization unconfirmed: requestId={}, targetStatus={}, exceptionType={}",
                    request.requestId(), target, failureType(writeFailure));
        }
    }

    private static String failureType(Throwable failure) {
        while (failure instanceof CompletionException && failure.getCause() != null) {
            failure = failure.getCause();
        }
        return failure.getClass().getSimpleName();
    }
}
