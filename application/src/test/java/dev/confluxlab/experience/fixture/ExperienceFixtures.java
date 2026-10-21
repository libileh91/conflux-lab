package dev.confluxlab.experience.fixture;

import dev.confluxlab.ConfluxLabApplication;
import dev.confluxlab.capability.api.FamilyRequests;
import dev.confluxlab.capability.internal.fixture.CapabilityFixtures;
import dev.confluxlab.contracts.CreateFamilyRequest;

public final class ExperienceFixtures {

    public static class UsesRepository {
        private CapabilityFixtures.Repository repository;
    }

    public static class UsesDocument {
        private CapabilityFixtures.Document document;
    }

    public static class InCycle {
        private CapabilityFixtures.InCycle capability;
    }

    public static class DependsOnApplication {
        private ConfluxLabApplication application;
    }

    public static class UsesPublicApi {
        private FamilyRequests familyRequests;
        private CreateFamilyRequest request;
    }

    private ExperienceFixtures() {
    }
}
