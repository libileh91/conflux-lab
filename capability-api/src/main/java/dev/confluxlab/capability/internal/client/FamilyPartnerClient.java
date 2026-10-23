package dev.confluxlab.capability.internal.client;

import java.util.UUID;

import dev.confluxlab.contracts.PersonPayload;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/** Synchronous HTTP contract and DTOs internal to the fictional family partner adapter. */
@FeignClient(name = "family-partner", url = "${conflux.partner.base-url}",
        configuration = FamilyPartnerFeignConfiguration.class)
public interface FamilyPartnerClient {

    @PostMapping(value = "/principals", consumes = "application/json", produces = "application/json")
    IdResponse createPrincipal(@RequestHeader("X-Request-Id") UUID requestId,
            @RequestBody PersonPayload principal);

    @PostMapping(value = "/contracts", consumes = "application/json", produces = "application/json")
    IdResponse openContract(@RequestHeader("X-Request-Id") UUID requestId,
            @RequestBody OpenContractRequest request);

    @PostMapping(value = "/dependents", consumes = "application/json", produces = "application/json")
    IdResponse createDependent(@RequestHeader("X-Request-Id") UUID requestId,
            @RequestBody CreateDependentRequest request);

    @PostMapping(value = "/contracts/{contractId}/dependents", consumes = "application/json")
    void attachDependent(@RequestHeader("X-Request-Id") UUID requestId,
            @PathVariable("contractId") String contractId, @RequestBody AttachDependentRequest request);

    record IdResponse(String id) {}

    record OpenContractRequest(String principalId) {}

    record CreateDependentRequest(String principalId, PersonPayload person) {}

    record AttachDependentRequest(String dependentId) {}
}
