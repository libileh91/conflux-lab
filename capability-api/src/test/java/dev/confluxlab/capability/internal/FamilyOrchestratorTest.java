package dev.confluxlab.capability.internal;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import dev.confluxlab.capability.internal.FamilyOperations.ContractId;
import dev.confluxlab.capability.internal.FamilyOperations.DependentId;
import dev.confluxlab.capability.internal.FamilyOperations.PrincipalId;
import dev.confluxlab.contracts.CreateFamilyRequest;
import dev.confluxlab.contracts.PersonPayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class FamilyOrchestratorTest {

    private static final PersonPayload PRINCIPAL = new PersonPayload("Alex", "Exemple", LocalDate.of(1990, 4, 12));
    private static final PersonPayload FIRST_PERSON = new PersonPayload("Camille", "Exemple", LocalDate.of(2015, 8, 21));
    private static final PersonPayload SECOND_PERSON = new PersonPayload("Noa", "Exemple", LocalDate.of(2018, 2, 3));
    private static final PrincipalId PRINCIPAL_ID = new PrincipalId("principal-1");
    private static final ContractId CONTRACT_ID = new ContractId("contract-1");
    private static final DependentId FIRST_ID = new DependentId("dependent-1");
    private static final DependentId SECOND_ID = new DependentId("dependent-2");

    @ParameterizedTest(name = "first dependent finishes first: {0}")
    @ValueSource(booleans = {true, false})
    @Timeout(value = 5, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void startsEachAttachmentAsSoonAsItsOwnPrerequisitesAreReady(boolean firstDependentFinishesFirst) throws Exception {
        UUID requestId = UUID.randomUUID();
        ControlledFamilyOperations controlled = new ControlledFamilyOperations();
        FamilyOrchestrator orchestrator = new FamilyOrchestrator(controlled.operations);
        CreateFamilyRequest request = new CreateFamilyRequest(PRINCIPAL, List.of(FIRST_PERSON, SECOND_PERSON));

        CompletableFuture<Void> result = orchestrator.execute(requestId, request);
        verify(controlled.operations).createPrincipal(requestId, PRINCIPAL);
        verifyNoMoreInteractions(controlled.operations);
        assertFalse(result.isDone());

        controlled.principal.complete(PRINCIPAL_ID);
        verify(controlled.operations).openContract(requestId, PRINCIPAL_ID);
        verify(controlled.operations).createFirstDependent(requestId, PRINCIPAL_ID, FIRST_PERSON);
        verify(controlled.operations).createSecondDependent(requestId, PRINCIPAL_ID, SECOND_PERSON);
        verifyNoMoreInteractions(controlled.operations);

        CompletableFuture<DependentId> earlyDependent = firstDependentFinishesFirst
                ? controlled.firstDependent : controlled.secondDependent;
        CompletableFuture<DependentId> lateDependent = firstDependentFinishesFirst
                ? controlled.secondDependent : controlled.firstDependent;
        CompletableFuture<Void> earlyAttachment = firstDependentFinishesFirst
                ? controlled.firstAttachment : controlled.secondAttachment;
        CompletableFuture<Void> lateAttachment = firstDependentFinishesFirst
                ? controlled.secondAttachment : controlled.firstAttachment;
        earlyDependent.complete(firstDependentFinishesFirst ? FIRST_ID : SECOND_ID);
        verifyNoMoreInteractions(controlled.operations);
        assertFalse(result.isDone());

        controlled.contract.complete(CONTRACT_ID);
        verifyAttachment(controlled, requestId, firstDependentFinishesFirst);
        verifyNoMoreInteractions(controlled.operations);
        assertFalse(lateDependent.isDone(), "The independent branch must still be pending.");
        earlyAttachment.complete(null);
        assertFalse(result.isDone(), "One completed attachment must not finish the whole graph.");

        lateDependent.complete(firstDependentFinishesFirst ? SECOND_ID : FIRST_ID);
        verifyAttachment(controlled, requestId, !firstDependentFinishesFirst);
        verifyNoMoreInteractions(controlled.operations);
        assertFalse(result.isDone(), "The second attachment must actually finish, not merely start.");
        lateAttachment.complete(null);
        assertNull(result.get(1, TimeUnit.SECONDS));
    }

    @Test
    @Timeout(value = 5, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void waitsForStartedBranchesAndPropagatesContractFailureWithoutStartingAttachments() throws Exception {
        UUID requestId = UUID.randomUUID();
        ControlledFamilyOperations controlled = new ControlledFamilyOperations();
        FamilyOrchestrator orchestrator = new FamilyOrchestrator(controlled.operations);
        CreateFamilyRequest request = new CreateFamilyRequest(PRINCIPAL, List.of(FIRST_PERSON, SECOND_PERSON));

        CompletableFuture<Void> result = orchestrator.execute(requestId, request);
        controlled.principal.complete(PRINCIPAL_ID);
        verify(controlled.operations).createPrincipal(requestId, PRINCIPAL);
        verify(controlled.operations).openContract(requestId, PRINCIPAL_ID);
        verify(controlled.operations).createFirstDependent(requestId, PRINCIPAL_ID, FIRST_PERSON);
        verify(controlled.operations).createSecondDependent(requestId, PRINCIPAL_ID, SECOND_PERSON);
        verifyNoMoreInteractions(controlled.operations);

        IllegalStateException failure = new IllegalStateException("Simulated contract creation failure.");
        controlled.contract.completeExceptionally(failure);
        assertFalse(controlled.firstDependent.isDone(), "The first started branch must not be cancelled.");
        assertFalse(controlled.secondDependent.isDone(), "The second started branch must not be cancelled.");
        assertFalse(result.isDone(), "The failed graph must still await its started branches.");
        verifyNoMoreInteractions(controlled.operations);

        controlled.firstDependent.complete(FIRST_ID);
        assertFalse(result.isDone(), "The second dependent is still in flight.");
        verifyNoMoreInteractions(controlled.operations);

        controlled.secondDependent.complete(SECOND_ID);
        ExecutionException propagated = assertThrows(ExecutionException.class, () -> result.get(1, TimeUnit.SECONDS));
        assertSame(failure, propagated.getCause());
        verifyNoMoreInteractions(controlled.operations);
    }

    private static void verifyAttachment(ControlledFamilyOperations controlled, UUID requestId, boolean first) {
        if (first) {
            verify(controlled.operations).attachFirstDependent(requestId, CONTRACT_ID, FIRST_ID);
        } else {
            verify(controlled.operations).attachSecondDependent(requestId, CONTRACT_ID, SECOND_ID);
        }
    }
}
