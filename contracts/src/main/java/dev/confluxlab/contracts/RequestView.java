package dev.confluxlab.contracts;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record RequestView(
        @NotNull UUID requestId,
        @NotNull RequestStatus status,
        @NotNull Instant createdAt) {
}
