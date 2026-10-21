package dev.confluxlab.capability.api;

import java.util.UUID;

import dev.confluxlab.contracts.CreateFamilyRequest;
import dev.confluxlab.contracts.RequestAccepted;
import dev.confluxlab.contracts.RequestView;

public interface FamilyRequests {

    RequestAccepted createFamily(CreateFamilyRequest request);

    RequestView getRequest(UUID requestId);
}
