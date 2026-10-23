package dev.confluxlab.capability.internal.client;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

import dev.confluxlab.capability.internal.orchestration.FamilyOperations;
import dev.confluxlab.capability.internal.orchestration.FamilyOrchestrator;
import dev.confluxlab.contracts.CreateFamilyRequest;

/** Test-only local comparison, not an alternative production orchestration. */
final class FamilyGraphMeasurements {

    private static final String SEQ = "seq";
    private static final String CONCURRENT = "concurrent";

    private FamilyGraphMeasurements() {}

    record Sample(String mode, int run, double millis) {}

    /** Runs on the test driver; only operation suppliers are submitted to the executor. */
    static List<Sample> measure(FamilyOperations operations, Executor executor, CreateFamilyRequest payload)
            throws Exception {
        FamilyOrchestrator orchestrator = new FamilyOrchestrator(operations, executor);
        sample(SEQ, 0, operations, executor, payload, orchestrator);
        sample(CONCURRENT, 0, operations, executor, payload, orchestrator);

        List<Sample> samples = new ArrayList<>(6);
        for (int run = 1; run <= 3; run++) {
            List<String> order = run == 2 ? List.of(CONCURRENT, SEQ) : List.of(SEQ, CONCURRENT);
            for (String mode : order) {
                samples.add(sample(mode, run, operations, executor, payload, orchestrator));
            }
        }
        return List.copyOf(samples);
    }

    private static Sample sample(String mode, int run, FamilyOperations operations, Executor executor,
            CreateFamilyRequest payload, FamilyOrchestrator orchestrator) throws Exception {
        UUID requestId = UUID.randomUUID();
        long started = System.nanoTime();
        CompletableFuture<Void> completion = SEQ.equals(mode)
                ? sequential(operations, executor, requestId, payload)
                : orchestrator.execute(requestId, payload);
        completion.get(10, TimeUnit.SECONDS);
        return new Sample(mode, run, (System.nanoTime() - started) / 1_000_000.0);
    }

    private static CompletableFuture<Void> sequential(FamilyOperations operations, Executor executor,
            UUID requestId, CreateFamilyRequest payload) {
        return submit(() -> operations.createPrincipal(requestId, payload.principal()), executor)
                .thenCompose(principalId -> {
                    return submit(() -> operations.openContract(requestId, principalId), executor)
                            .thenCompose(contractId -> {
                                return submit(() -> operations.createFirstDependent(
                                        requestId, principalId, payload.dependents().get(0)), executor)
                                        .thenCompose(firstDependentId -> {
                                            return submit(() -> operations.createSecondDependent(
                                                    requestId, principalId, payload.dependents().get(1)), executor)
                                                    .thenCompose(secondDependentId ->
                                                            submit(() -> operations.attachFirstDependent(
                                                                    requestId, contractId, firstDependentId), executor)
                                                                    .thenCompose(ignored -> submit(
                                                                            () -> operations.attachSecondDependent(
                                                                                    requestId, contractId, secondDependentId),
                                                                            executor)));
                                        });
                            });
                });
    }

    private static <T> CompletableFuture<T> submit(Supplier<CompletableFuture<T>> operation, Executor executor) {
        return CompletableFuture.supplyAsync(operation, executor).thenCompose(Function.identity());
    }
}
