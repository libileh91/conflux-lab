package dev.confluxlab.capability.internal;

import java.util.Objects;
import java.util.UUID;

record FamilyRequestSubmitted(UUID requestId) {

    FamilyRequestSubmitted {
        Objects.requireNonNull(requestId, "requestId");
    }
}
