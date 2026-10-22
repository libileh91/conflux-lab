package dev.confluxlab.capability.internal;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import dev.confluxlab.capability.internal.FamilyOperations.ContractId;
import dev.confluxlab.capability.internal.FamilyOperations.DependentId;
import dev.confluxlab.capability.internal.FamilyOperations.PrincipalId;
import dev.confluxlab.contracts.CreateFamilyRequest;
import dev.confluxlab.contracts.PersonPayload;

final class FamilyOrchestrator {

    private final FamilyOperations operations;

    FamilyOrchestrator(FamilyOperations operations) {
        this.operations = Objects.requireNonNull(operations, "operations");
    }

    CompletableFuture<Void> execute(UUID requestId, CreateFamilyRequest request) {
        PersonPayload firstPerson = request.dependents().get(0);
        PersonPayload secondPerson = request.dependents().get(1);
        CompletableFuture<PrincipalId> principal = operations.createPrincipal(requestId, request.principal());
        CompletableFuture<ContractId> contract = principal.thenCompose(
                principalId -> operations.openContract(requestId, principalId));
        CompletableFuture<DependentId> firstDependent = principal.thenCompose(
                principalId -> operations.createFirstDependent(requestId, principalId, firstPerson));
        CompletableFuture<DependentId> secondDependent = principal.thenCompose(
                principalId -> operations.createSecondDependent(requestId, principalId, secondPerson));

        CompletableFuture<Void> firstAttachment = contract.thenCombine(firstDependent,
                        (contractId, dependentId) -> operations.attachFirstDependent(requestId, contractId, dependentId))
                .thenCompose(future -> future);
        CompletableFuture<Void> secondAttachment = contract.thenCombine(secondDependent,
                        (contractId, dependentId) -> operations.attachSecondDependent(requestId, contractId, dependentId))
                .thenCompose(future -> future);
        // Settle started branches before the caller can finalize, even if attachments are skipped after a failure.
        return CompletableFuture.allOf(contract, firstDependent, secondDependent, firstAttachment, secondAttachment);
    }
}
