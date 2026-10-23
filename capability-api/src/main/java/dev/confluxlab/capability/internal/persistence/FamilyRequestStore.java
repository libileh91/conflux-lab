package dev.confluxlab.capability.internal.persistence;

import java.util.Optional;
import java.util.UUID;

public interface FamilyRequestStore {

    void save(StoredFamilyRequest request);

    Optional<StoredFamilyRequest> findById(UUID requestId);
}
