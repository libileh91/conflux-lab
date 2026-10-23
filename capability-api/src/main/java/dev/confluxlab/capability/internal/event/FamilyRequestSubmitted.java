package dev.confluxlab.capability.internal.event;

import java.util.Objects;
import java.util.UUID;

public record FamilyRequestSubmitted(UUID requestId) {

    public FamilyRequestSubmitted {
        Objects.requireNonNull(requestId, "requestId");
    }
}
