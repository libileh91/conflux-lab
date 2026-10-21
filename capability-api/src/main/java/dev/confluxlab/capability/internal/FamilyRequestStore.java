package dev.confluxlab.capability.internal;

import java.util.Optional;
import java.util.UUID;

interface FamilyRequestStore {

    void save(StoredFamilyRequest request);

    Optional<StoredFamilyRequest> findById(UUID requestId);
}
