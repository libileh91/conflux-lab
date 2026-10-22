package dev.confluxlab.capability.internal;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import dev.confluxlab.capability.internal.FamilyOperations.ContractId;
import dev.confluxlab.capability.internal.FamilyOperations.DependentId;
import dev.confluxlab.capability.internal.FamilyOperations.PrincipalId;
import dev.confluxlab.contracts.PersonPayload;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class ControlledFamilyOperations {

    final FamilyOperations operations = mock(FamilyOperations.class);
    final CompletableFuture<PrincipalId> principal = new CompletableFuture<>();
    final CompletableFuture<ContractId> contract = new CompletableFuture<>();
    final CompletableFuture<DependentId> firstDependent = new CompletableFuture<>();
    final CompletableFuture<DependentId> secondDependent = new CompletableFuture<>();
    final CompletableFuture<Void> firstAttachment = new CompletableFuture<>();
    final CompletableFuture<Void> secondAttachment = new CompletableFuture<>();

    ControlledFamilyOperations() {
        when(operations.createPrincipal(any(UUID.class), any(PersonPayload.class))).thenReturn(principal);
        when(operations.openContract(any(UUID.class), any(PrincipalId.class))).thenReturn(contract);
        when(operations.createFirstDependent(any(UUID.class), any(PrincipalId.class), any(PersonPayload.class)))
                .thenReturn(firstDependent);
        when(operations.createSecondDependent(any(UUID.class), any(PrincipalId.class), any(PersonPayload.class)))
                .thenReturn(secondDependent);
        when(operations.attachFirstDependent(any(UUID.class), any(ContractId.class), any(DependentId.class)))
                .thenReturn(firstAttachment);
        when(operations.attachSecondDependent(any(UUID.class), any(ContractId.class), any(DependentId.class)))
                .thenReturn(secondAttachment);
    }
}
