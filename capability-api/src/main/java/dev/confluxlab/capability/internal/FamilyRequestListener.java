package dev.confluxlab.capability.internal;

import java.time.Clock;
import java.time.Instant;

import dev.confluxlab.contracts.RequestStatus;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
class FamilyRequestListener {

    private final MongoFamilyRequestStore store;
    private final Clock clock;

    FamilyRequestListener(MongoFamilyRequestStore store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    @Async("familyRequestExecutor")
    @EventListener
    public void onFamilyRequestSubmitted(FamilyRequestSubmitted event) {
        store.findById(event.requestId())
                .filter(request -> request.status() == RequestStatus.PENDING)
                .ifPresent(request -> {
                    // This increment stops at the atomic claim; no business processing is simulated.
                    store.transition(request.requestId(), RequestStatus.PENDING, RequestStatus.PROCESSING, Instant.now(clock));
                });
    }
}
