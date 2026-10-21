package dev.confluxlab.contracts;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateFamilyRequest(
        @NotNull @Valid PersonPayload principal,
        @NotNull @Size(min = 2, max = 2) List<@NotNull @Valid PersonPayload> dependents) {
}
