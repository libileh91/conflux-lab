package dev.confluxlab.capability.internal;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

@Repository
class MongoFamilyRequestStore implements FamilyRequestStore {

    private final MongoTemplate mongoTemplate;

    MongoFamilyRequestStore(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void save(StoredFamilyRequest request) {
        // Insert, not upsert: an existing UUID must never be overwritten during acceptance.
        mongoTemplate.insert(request);
    }

    @Override
    public Optional<StoredFamilyRequest> findById(UUID requestId) {
        return Optional.ofNullable(mongoTemplate.findById(requestId, StoredFamilyRequest.class));
    }
}
