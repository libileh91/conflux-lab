package dev.confluxlab.capability.internal.orchestration;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Function;
import java.util.function.Supplier;

import dev.confluxlab.capability.internal.orchestration.FamilyOperations.ContractId;
import dev.confluxlab.capability.internal.orchestration.FamilyOperations.DependentId;
import dev.confluxlab.capability.internal.orchestration.FamilyOperations.PrincipalId;
import dev.confluxlab.contracts.CreateFamilyRequest;
import dev.confluxlab.contracts.PersonPayload;

public final class FamilyOrchestrator {

    private final FamilyOperations operations;
    private final Executor executor;

    public FamilyOrchestrator(FamilyOperations operations, Executor executor) {
        this.operations = Objects.requireNonNull(operations, "operations");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    public CompletableFuture<Void> execute(UUID requestId, CreateFamilyRequest request) {
        PersonPayload firstPerson = request.dependents().get(0);
        PersonPayload secondPerson = request.dependents().get(1);
        CompletableFuture<PrincipalId> principal = submit(() -> operations.createPrincipal(requestId, request.principal()));
        CompletableFuture<ContractId> contract = principal.thenCompose(
                principalId -> submit(() -> operations.openContract(requestId, principalId)));
        CompletableFuture<DependentId> firstDependent = principal.thenCompose(
                principalId -> submit(() -> operations.createFirstDependent(requestId, principalId, firstPerson)));
        CompletableFuture<DependentId> secondDependent = principal.thenCompose(
                principalId -> submit(() -> operations.createSecondDependent(requestId, principalId, secondPerson)));

        CompletableFuture<Void> firstAttachment = contract.thenCombine(firstDependent,
                        (contractId, dependentId) -> submit(() -> operations.attachFirstDependent(requestId, contractId, dependentId)))
                .thenCompose(future -> future);
        CompletableFuture<Void> secondAttachment = contract.thenCombine(secondDependent,
                        (contractId, dependentId) -> submit(() -> operations.attachSecondDependent(requestId, contractId, dependentId)))
                .thenCompose(future -> future);
        // Settle started branches before the caller can finalize, even if attachments are skipped after a failure.
        return CompletableFuture.allOf(contract, firstDependent, secondDependent, firstAttachment, secondAttachment);
    }

    private <T> CompletableFuture<T> submit(Supplier<CompletableFuture<T>> operation) {
        try {
            return CompletableFuture.supplyAsync(operation, executor).thenCompose(Function.identity());
        } catch (RejectedExecutionException rejected) {
            // Initial submission can fail before supplyAsync returns a future.
            return CompletableFuture.failedFuture(rejected);
        }
    }
}
