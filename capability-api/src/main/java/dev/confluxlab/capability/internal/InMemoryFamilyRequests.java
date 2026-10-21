package dev.confluxlab.capability.internal;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

import dev.confluxlab.capability.api.FamilyRequests;
import dev.confluxlab.capability.api.InvalidFamilyRequestException;
import dev.confluxlab.capability.api.RequestNotFoundException;
import dev.confluxlab.contracts.CreateFamilyRequest;
import dev.confluxlab.contracts.RequestAccepted;
import dev.confluxlab.contracts.RequestStatus;
import dev.confluxlab.contracts.RequestView;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;

@Service
class InMemoryFamilyRequests implements FamilyRequests {

    private final InMemoryFamilyRequestStore store;
    private final Validator validator;
    private final Clock clock;

    InMemoryFamilyRequests(InMemoryFamilyRequestStore store, Validator validator, Clock clock) {
        this.store = store;
        this.validator = validator;
        this.clock = clock;
    }

    @Override
    public RequestAccepted createFamily(CreateFamilyRequest request) {
        if (request == null) {
            throw new InvalidFamilyRequestException();
        }
        // Validate a local snapshot; the stored record then freezes its list before publication.
        CreateFamilyRequest snapshot = new CreateFamilyRequest(request.principal(),
                request.dependents() == null ? null : new ArrayList<>(request.dependents()));
        if (!validator.validate(snapshot).isEmpty()) {
            throw new InvalidFamilyRequestException();
        }

        UUID requestId = UUID.randomUUID();
        StoredFamilyRequest stored = new StoredFamilyRequest(
                requestId, RequestStatus.PENDING, snapshot, Instant.now(clock));
        store.save(stored);
        return new RequestAccepted(requestId, stored.status(), "/api/v1/requests/" + requestId);
    }

    @Override
    public RequestView getRequest(UUID requestId) {
        if (requestId == null) {
            throw new IllegalArgumentException("requestId must not be null.");
        }
        StoredFamilyRequest stored = store.findById(requestId)
                .orElseThrow(RequestNotFoundException::new);
        return new RequestView(stored.requestId(), stored.status(), stored.createdAt());
    }
}
