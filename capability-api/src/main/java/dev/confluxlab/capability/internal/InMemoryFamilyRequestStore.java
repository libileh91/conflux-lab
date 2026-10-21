package dev.confluxlab.capability.internal;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Repository;

@Repository
class InMemoryFamilyRequestStore {

    private final ConcurrentMap<UUID, StoredFamilyRequest> requests = new ConcurrentHashMap<>();

    void save(StoredFamilyRequest request) {
        if (requests.putIfAbsent(request.requestId(), request) != null) {
            throw new IllegalStateException("Request identifier already exists.");
        }
    }

    Optional<StoredFamilyRequest> findById(UUID requestId) {
        return Optional.ofNullable(requests.get(requestId));
    }
}
