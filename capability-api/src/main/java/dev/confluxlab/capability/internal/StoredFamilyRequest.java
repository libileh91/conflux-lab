package dev.confluxlab.capability.internal;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import dev.confluxlab.contracts.CreateFamilyRequest;
import dev.confluxlab.contracts.RequestStatus;

record StoredFamilyRequest(UUID requestId, RequestStatus status, CreateFamilyRequest payload, Instant createdAt) {

    StoredFamilyRequest {
        payload = new CreateFamilyRequest(payload.principal(), List.copyOf(payload.dependents()));
    }
}
