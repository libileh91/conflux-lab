package dev.confluxlab.capability.internal;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

class InMemoryFamilyRequestStore implements FamilyRequestStore {

    private final ConcurrentMap<UUID, StoredFamilyRequest> requests = new ConcurrentHashMap<>();

    @Override
    public void save(StoredFamilyRequest request) {
        if (requests.putIfAbsent(request.requestId(), request) != null) {
            throw new IllegalStateException("Request identifier already exists.");
        }
    }

    @Override
    public Optional<StoredFamilyRequest> findById(UUID requestId) {
        return Optional.ofNullable(requests.get(requestId));
    }
}
