package dev.confluxlab.capability.internal.orchestration;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import dev.confluxlab.contracts.PersonPayload;

/** Internal contracts for the six fictional operations, not the public HTTP contract. */
public interface FamilyOperations {

    CompletableFuture<PrincipalId> createPrincipal(UUID requestId, PersonPayload principal);

    CompletableFuture<ContractId> openContract(UUID requestId, PrincipalId principalId);

    CompletableFuture<DependentId> createFirstDependent(UUID requestId, PrincipalId principalId, PersonPayload dependent);

    CompletableFuture<DependentId> createSecondDependent(UUID requestId, PrincipalId principalId, PersonPayload dependent);

    CompletableFuture<Void> attachFirstDependent(UUID requestId, ContractId contractId, DependentId dependentId);

    CompletableFuture<Void> attachSecondDependent(UUID requestId, ContractId contractId, DependentId dependentId);

    record PrincipalId(String value) {}

    record ContractId(String value) {}

    record DependentId(String value) {}
}
