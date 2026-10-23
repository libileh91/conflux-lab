package dev.confluxlab.capability.internal.persistence;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import dev.confluxlab.contracts.RequestStatus;

import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
public class MongoFamilyRequestStore implements FamilyRequestStore {

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

    public Optional<StoredFamilyRequest> transition(
        UUID requestId,
        RequestStatus expectedStatus,
        RequestStatus nextStatus,
        Instant changedAt
    ) {
        Objects.requireNonNull(requestId, "requestId");
        StoredFamilyRequest.requireTransition(expectedStatus, nextStatus);
        Instant timestamp = Objects.requireNonNull(changedAt, "changedAt").truncatedTo(ChronoUnit.MILLIS);
        Query query = Query.query(
            Criteria.where("_id").is(requestId).and("status").is(expectedStatus).and("updatedAt").lte(timestamp)
        );
        Update update = new Update().set("status", nextStatus).set("updatedAt", timestamp);
        if (nextStatus == RequestStatus.PROCESSING) {
            update.unset("completedAt");
        } else {
            update.set("completedAt", timestamp);
        }
        // The expected status and the update must be evaluated together by MongoDB, not by a Java read/save pair.
        return Optional.ofNullable(
            mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true).upsert(false),
                StoredFamilyRequest.class
            )
        );
    }
}
