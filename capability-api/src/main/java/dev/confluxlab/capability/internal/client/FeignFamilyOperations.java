package dev.confluxlab.capability.internal.client;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import dev.confluxlab.capability.internal.client.FamilyPartnerClient.AttachDependentRequest;
import dev.confluxlab.capability.internal.client.FamilyPartnerClient.CreateDependentRequest;
import dev.confluxlab.capability.internal.client.FamilyPartnerClient.IdResponse;
import dev.confluxlab.capability.internal.client.FamilyPartnerClient.OpenContractRequest;
import dev.confluxlab.capability.internal.orchestration.FamilyOperations;
import dev.confluxlab.capability.internal.orchestration.FamilyOperations.ContractId;
import dev.confluxlab.capability.internal.orchestration.FamilyOperations.DependentId;
import dev.confluxlab.capability.internal.orchestration.FamilyOperations.PrincipalId;
import dev.confluxlab.contracts.PersonPayload;

import org.springframework.stereotype.Component;

@Component
class FeignFamilyOperations implements FamilyOperations {

    private final FamilyPartnerClient client;

    FeignFamilyOperations(FamilyPartnerClient client) {
        this.client = client;
    }

    @Override
    public CompletableFuture<PrincipalId> createPrincipal(UUID requestId, PersonPayload principal) {
        return invoke(() -> new PrincipalId(requireId(client.createPrincipal(requestId, principal))));
    }

    @Override
    public CompletableFuture<ContractId> openContract(UUID requestId, PrincipalId principalId) {
        return invoke(() -> new ContractId(requireId(
                client.openContract(requestId, new OpenContractRequest(principalId.value())))));
    }

    @Override
    public CompletableFuture<DependentId> createFirstDependent(
            UUID requestId, PrincipalId principalId, PersonPayload dependent) {
        return createDependent(requestId, principalId, dependent);
    }

    @Override
    public CompletableFuture<DependentId> createSecondDependent(
            UUID requestId, PrincipalId principalId, PersonPayload dependent) {
        return createDependent(requestId, principalId, dependent);
    }

    @Override
    public CompletableFuture<Void> attachFirstDependent(
            UUID requestId, ContractId contractId, DependentId dependentId) {
        return attachDependent(requestId, contractId, dependentId);
    }

    @Override
    public CompletableFuture<Void> attachSecondDependent(
            UUID requestId, ContractId contractId, DependentId dependentId) {
        return attachDependent(requestId, contractId, dependentId);
    }

    private CompletableFuture<DependentId> createDependent(
            UUID requestId, PrincipalId principalId, PersonPayload dependent) {
        return invoke(() -> new DependentId(requireId(
                client.createDependent(requestId, new CreateDependentRequest(principalId.value(), dependent)))));
    }

    private CompletableFuture<Void> attachDependent(
            UUID requestId, ContractId contractId, DependentId dependentId) {
        return invoke(() -> {
            client.attachDependent(requestId, contractId.value(), new AttachDependentRequest(dependentId.value()));
            return null;
        });
    }

    private static String requireId(IdResponse response) {
        if (response == null || response.id() == null || response.id().isBlank()) {
            throw new IllegalStateException("Family partner returned a missing identifier.");
        }
        return response.id();
    }

    private static <T> CompletableFuture<T> invoke(Supplier<T> operation) {
        return CompletableFuture.completedFuture(operation.get());
    }
}
