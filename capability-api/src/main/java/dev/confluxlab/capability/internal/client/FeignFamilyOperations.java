package dev.confluxlab.capability.internal.client;

import java.net.SocketTimeoutException;
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

import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class FeignFamilyOperations implements FamilyOperations {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeignFamilyOperations.class);

    private final FamilyPartnerClient client;

    FeignFamilyOperations(FamilyPartnerClient client) {
        this.client = client;
    }

    @Override
    public CompletableFuture<PrincipalId> createPrincipal(UUID requestId, PersonPayload principal) {
        return invoke(requestId, "A", () -> new PrincipalId(requireId(client.createPrincipal(requestId, principal))));
    }

    @Override
    public CompletableFuture<ContractId> openContract(UUID requestId, PrincipalId principalId) {
        return invoke(requestId, "B", () -> new ContractId(requireId(
                client.openContract(requestId, new OpenContractRequest(principalId.value())))));
    }

    @Override
    public CompletableFuture<DependentId> createFirstDependent(
            UUID requestId, PrincipalId principalId, PersonPayload dependent) {
        return createDependent(requestId, "C", principalId, dependent);
    }

    @Override
    public CompletableFuture<DependentId> createSecondDependent(
            UUID requestId, PrincipalId principalId, PersonPayload dependent) {
        return createDependent(requestId, "D", principalId, dependent);
    }

    @Override
    public CompletableFuture<Void> attachFirstDependent(
            UUID requestId, ContractId contractId, DependentId dependentId) {
        return attachDependent(requestId, "E", contractId, dependentId);
    }

    @Override
    public CompletableFuture<Void> attachSecondDependent(
            UUID requestId, ContractId contractId, DependentId dependentId) {
        return attachDependent(requestId, "F", contractId, dependentId);
    }

    private CompletableFuture<DependentId> createDependent(
            UUID requestId, String operationName, PrincipalId principalId, PersonPayload dependent) {
        return invoke(requestId, operationName, () -> new DependentId(requireId(
                client.createDependent(requestId, new CreateDependentRequest(principalId.value(), dependent)))));
    }

    private CompletableFuture<Void> attachDependent(
            UUID requestId, String operationName, ContractId contractId, DependentId dependentId) {
        return invoke(requestId, operationName, () -> {
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

    private static <T> CompletableFuture<T> invoke(UUID requestId, String operationName, Supplier<T> operation) {
        try {
            return CompletableFuture.completedFuture(operation.get());
        } catch (RuntimeException failure) {
            int httpStatus = failure instanceof FeignException response ? response.status() : -1;
            // Never log exception messages, URLs, bodies or stack traces: they may contain family data.
            LOGGER.error("Family partner call failed: requestId={}, operation={}, failureKind={}, httpStatus={}, exceptionType={}",
                    requestId, operationName, failureKind(failure, httpStatus), httpStatus, failure.getClass().getSimpleName());
            throw failure;
        }
    }

    private static String failureKind(RuntimeException failure, int httpStatus) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof SocketTimeoutException) {
                return "TIMEOUT";
            }
        }
        if (httpStatus == 408 || httpStatus == 429 || httpStatus >= 500) {
            return "TRANSIENT_HTTP_ERROR";
        }
        if (httpStatus >= 400) {
            return "REJECTED";
        }
        return failure instanceof FeignException && httpStatus < 0 ? "TRANSPORT_ERROR" : "UNEXPECTED_ERROR";
    }
}
