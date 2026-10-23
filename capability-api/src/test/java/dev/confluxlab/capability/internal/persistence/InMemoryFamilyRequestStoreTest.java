package dev.confluxlab.capability.internal.persistence;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import dev.confluxlab.contracts.CreateFamilyRequest;
import dev.confluxlab.contracts.PersonPayload;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryFamilyRequestStoreTest {

    @Test
    void startsEmpty() {
        assertTrue(new InMemoryFamilyRequestStore().findById(UUID.randomUUID()).isEmpty());
    }

    @Test
    void retrievesTheSavedRequestByItsId() {
        InMemoryFamilyRequestStore store = new InMemoryFamilyRequestStore();
        StoredFamilyRequest request = storedRequest(UUID.randomUUID());

        store.save(request);

        assertSame(request, store.findById(request.requestId()).orElseThrow());
    }

    @Test
    void rejectsAnIdentifierCollisionWithoutOverwritingTheOriginalRequest() {
        InMemoryFamilyRequestStore store = new InMemoryFamilyRequestStore();
        StoredFamilyRequest original = storedRequest(UUID.randomUUID());
        StoredFamilyRequest duplicate = storedRequest(original.requestId());
        store.save(original);

        assertThrows(IllegalStateException.class, () -> store.save(duplicate));
        assertSame(original, store.findById(original.requestId()).orElseThrow());
    }

    private static StoredFamilyRequest storedRequest(UUID requestId) {
        CreateFamilyRequest payload = new CreateFamilyRequest(
                new PersonPayload("Alex", "Exemple", LocalDate.of(1990, 4, 12)),
                List.of(new PersonPayload("Camille", "Exemple", LocalDate.of(2015, 8, 21)),
                        new PersonPayload("Noa", "Exemple", LocalDate.of(2018, 2, 3))));
        return StoredFamilyRequest.pending(requestId, payload, Instant.parse("2026-10-21T12:00:00Z"));
    }
}
