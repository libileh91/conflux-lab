package dev.confluxlab.capability.internal.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import dev.confluxlab.contracts.CreateFamilyRequest;
import dev.confluxlab.contracts.RequestStatus;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("family_requests")
public record StoredFamilyRequest(
    @Id UUID requestId,
    RequestStatus status,
    CreateFamilyRequest payload,
    Instant createdAt,
    Instant updatedAt,
    Instant completedAt
) {
    public StoredFamilyRequest {
        Objects.requireNonNull(requestId, "requestId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
        if (
            updatedAt.isBefore(createdAt) ||
            isTerminal(status) != (completedAt != null) ||
            (completedAt != null && !completedAt.equals(updatedAt))
        ) {
            throw new IllegalArgumentException("Request dates must match its status and chronology.");
        }
        payload = new CreateFamilyRequest(payload.principal(), List.copyOf(payload.dependents()));
    }

    public static StoredFamilyRequest pending(UUID requestId, CreateFamilyRequest payload, Instant createdAt) {
        return new StoredFamilyRequest(requestId, RequestStatus.PENDING, payload, createdAt, createdAt, null);
    }

    public StoredFamilyRequest transitionTo(RequestStatus nextStatus, Instant changedAt) {
        requireTransition(status, nextStatus);
        Objects.requireNonNull(changedAt, "changedAt");
        if (changedAt.isBefore(updatedAt)) {
            throw new IllegalArgumentException("A transition cannot precede the last update.");
        }
        return new StoredFamilyRequest(
            requestId,
            nextStatus,
            payload,
            createdAt,
            changedAt,
            isTerminal(nextStatus) ? changedAt : null
        );
    }

    static void requireTransition(RequestStatus currentStatus, RequestStatus nextStatus) {
        Objects.requireNonNull(currentStatus, "currentStatus");
        Objects.requireNonNull(nextStatus, "nextStatus");
        boolean allowed = switch (currentStatus) {
            case PENDING -> nextStatus == RequestStatus.PROCESSING;
            case PROCESSING -> nextStatus == RequestStatus.SUCCEEDED || nextStatus == RequestStatus.FAILED;
            case SUCCEEDED, FAILED -> false;
        };
        if (!allowed) {
            throw new IllegalStateException("Invalid request status transition: " + currentStatus + " -> " + nextStatus);
        }
    }

    private static boolean isTerminal(RequestStatus status) {
        return status == RequestStatus.SUCCEEDED || status == RequestStatus.FAILED;
    }
}
