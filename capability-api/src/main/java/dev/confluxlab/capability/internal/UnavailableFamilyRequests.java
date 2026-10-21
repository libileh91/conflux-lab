package dev.confluxlab.capability.internal;

import java.util.UUID;

import dev.confluxlab.capability.api.CapabilityNotReadyException;
import dev.confluxlab.capability.api.FamilyRequests;
import dev.confluxlab.contracts.CreateFamilyRequest;
import dev.confluxlab.contracts.RequestAccepted;
import dev.confluxlab.contracts.RequestView;
import org.springframework.stereotype.Service;

// Assembly placeholder until INT-06/07 implement acceptance and tracking; never acknowledge lost work.
@Service
class UnavailableFamilyRequests implements FamilyRequests {

    @Override
    public RequestAccepted createFamily(CreateFamilyRequest request) {
        throw new CapabilityNotReadyException();
    }

    @Override
    public RequestView getRequest(UUID requestId) {
        throw new CapabilityNotReadyException();
    }
}
