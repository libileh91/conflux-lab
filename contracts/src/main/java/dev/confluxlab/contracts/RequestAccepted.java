package dev.confluxlab.contracts;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RequestAccepted(
        @NotNull UUID requestId,
        @NotNull RequestStatus status,
        @NotBlank String statusUrl) {
}
