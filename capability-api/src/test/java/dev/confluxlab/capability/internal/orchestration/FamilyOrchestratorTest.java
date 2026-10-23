package dev.confluxlab.capability.internal.orchestration;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import dev.confluxlab.capability.internal.config.FamilyRequestConfiguration;
import dev.confluxlab.capability.internal.orchestration.FamilyOperations.ContractId;
import dev.confluxlab.capability.internal.orchestration.FamilyOperations.DependentId;
import dev.confluxlab.capability.internal.orchestration.FamilyOperations.PrincipalId;
import dev.confluxlab.contracts.CreateFamilyRequest;
import dev.confluxlab.contracts.PersonPayload;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
        FamilyOrchestrator orchestrator = new FamilyOrchestrator(controlled.operations, Runnable::run);
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
        FamilyOrchestrator orchestrator = new FamilyOrchestrator(controlled.operations, Runnable::run);
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

    @ParameterizedTest(name = "initial submission is rejected: {0}")
    @ValueSource(booleans = {true, false})
    @Timeout(value = 5, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void propagatesSubmissionRejectionWithoutCallingTheRejectedOperation(boolean rejectInitially) throws Exception {
        UUID requestId = UUID.randomUUID();
        ControlledFamilyOperations controlled = new ControlledFamilyOperations();
        RejectedExecutionException rejection = new RejectedExecutionException("Simulated saturation.");
        AtomicBoolean reject = new AtomicBoolean(rejectInitially);
        Executor executor = task -> {
            if (reject.get()) {
                throw rejection;
            }
            task.run();
        };
        FamilyOrchestrator orchestrator = new FamilyOrchestrator(controlled.operations, executor);
        CreateFamilyRequest request = new CreateFamilyRequest(PRINCIPAL, List.of(FIRST_PERSON, SECOND_PERSON));

        CompletableFuture<Void> result = orchestrator.execute(requestId, request);
        if (rejectInitially) {
            verifyNoInteractions(controlled.operations);
        } else {
            controlled.principal.complete(PRINCIPAL_ID);
            verify(controlled.operations).createPrincipal(requestId, PRINCIPAL);
            verify(controlled.operations).openContract(requestId, PRINCIPAL_ID);
            verify(controlled.operations).createFirstDependent(requestId, PRINCIPAL_ID, FIRST_PERSON);
            verify(controlled.operations).createSecondDependent(requestId, PRINCIPAL_ID, SECOND_PERSON);

            reject.set(true);
            controlled.contract.complete(CONTRACT_ID);
            controlled.firstDependent.complete(FIRST_ID);
            assertFalse(result.isDone(), "A rejected attachment must not abandon the other started branch.");
            verifyNoMoreInteractions(controlled.operations);

            reject.set(false);
            controlled.secondDependent.complete(SECOND_ID);
            verify(controlled.operations).attachSecondDependent(requestId, CONTRACT_ID, SECOND_ID);
            assertFalse(result.isDone(), "The accepted attachment must actually finish.");
            controlled.secondAttachment.complete(null);
            verifyNoMoreInteractions(controlled.operations);
        }
        ExecutionException propagated = assertThrows(ExecutionException.class, () -> result.get(1, TimeUnit.SECONDS));
        assertSame(rejection, propagated.getCause());
    }

    @Test
    @Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void sharesTheBoundedPoolWithoutWaitingForSubtasksOrMixingRequests() throws Exception {
        ThreadPoolTaskExecutor executor = new FamilyRequestConfiguration().familyRequestExecutor(1, 16, 1);
        executor.initialize();
        try {
            UUID firstRequestId = UUID.randomUUID();
            UUID secondRequestId = UUID.randomUUID();
            ControlledFamilyOperations controlled = new ControlledFamilyOperations();
            CompletableFuture<PrincipalId> secondPrincipal = new CompletableFuture<>();
            CountDownLatch started = new CountDownLatch(2);
            ConcurrentLinkedQueue<Thread> workers = new ConcurrentLinkedQueue<>();
            doAnswer(invocation -> {
                workers.add(Thread.currentThread());
                started.countDown();
                return firstRequestId.equals(invocation.getArgument(0)) ? controlled.principal : secondPrincipal;
            }).when(controlled.operations).createPrincipal(any(UUID.class), eq(PRINCIPAL));
            FamilyOrchestrator orchestrator = new FamilyOrchestrator(controlled.operations, executor);
            CreateFamilyRequest request = new CreateFamilyRequest(PRINCIPAL, List.of(FIRST_PERSON, SECOND_PERSON));

            CompletableFuture<Void> firstResult = executor.submit(() -> orchestrator.execute(firstRequestId, request))
                    .get(1, TimeUnit.SECONDS);
            CompletableFuture<Void> secondResult = executor.submit(() -> orchestrator.execute(secondRequestId, request))
                    .get(1, TimeUnit.SECONDS);
            assertTrue(started.await(1, TimeUnit.SECONDS), "The only worker must be free to invoke both operations.");
            assertTrue(workers.stream().allMatch(worker -> worker.getName().startsWith("family-request-")));

            IllegalStateException failure = new IllegalStateException("Simulated second request failure.");
            secondPrincipal.completeExceptionally(failure);
            ExecutionException propagated = assertThrows(ExecutionException.class, () -> secondResult.get(1, TimeUnit.SECONDS));
            assertSame(failure, propagated.getCause());
            assertFalse(firstResult.isDone(), "The second request failure must not complete the first request.");

            controlled.contract.complete(CONTRACT_ID);
            controlled.firstDependent.complete(FIRST_ID);
            controlled.secondDependent.complete(SECOND_ID);
            controlled.firstAttachment.complete(null);
            controlled.secondAttachment.complete(null);
            controlled.principal.complete(PRINCIPAL_ID);
            assertNull(firstResult.get(1, TimeUnit.SECONDS));
            assertTrue(secondResult.isCompletedExceptionally());
            verify(controlled.operations).createPrincipal(firstRequestId, PRINCIPAL);
            verify(controlled.operations).createPrincipal(secondRequestId, PRINCIPAL);
            verify(controlled.operations).openContract(firstRequestId, PRINCIPAL_ID);
            verify(controlled.operations).createFirstDependent(firstRequestId, PRINCIPAL_ID, FIRST_PERSON);
            verify(controlled.operations).createSecondDependent(firstRequestId, PRINCIPAL_ID, SECOND_PERSON);
            verifyAttachment(controlled, firstRequestId, true);
            verifyAttachment(controlled, firstRequestId, false);
            verifyNoMoreInteractions(controlled.operations);
        } finally {
            executor.shutdown();
            assertTrue(executor.getThreadPoolExecutor().awaitTermination(2, TimeUnit.SECONDS));
        }
    }

    private static void verifyAttachment(ControlledFamilyOperations controlled, UUID requestId, boolean first) {
        if (first) {
            verify(controlled.operations).attachFirstDependent(requestId, CONTRACT_ID, FIRST_ID);
        } else {
            verify(controlled.operations).attachSecondDependent(requestId, CONTRACT_ID, SECOND_ID);
        }
    }
}
