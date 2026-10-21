package dev.confluxlab.capability.internal.fixture;

import dev.confluxlab.experience.FamilyController;
import dev.confluxlab.experience.fixture.ExperienceFixtures;

// These deliberately invalid dependency graphs are imported only by the rule-detection tests.
public final class CapabilityFixtures {

    public static class Repository {
    }

    public static class Document {
    }

    public static class DependsOnBff {
        private FamilyController controller;
    }

    public static class InCycle {
        private ExperienceFixtures.InCycle experience;
    }

    private CapabilityFixtures() {
    }
}
