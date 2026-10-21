package dev.confluxlab;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import dev.confluxlab.capability.api.FamilyRequests;
import dev.confluxlab.capability.internal.fixture.CapabilityFixtures;
import dev.confluxlab.contracts.CreateFamilyRequest;
import dev.confluxlab.contracts.PersonPayload;
import dev.confluxlab.contracts.RequestAccepted;
import dev.confluxlab.contracts.RequestStatus;
import dev.confluxlab.contracts.RequestView;
import dev.confluxlab.contracts.fixture.ContractsFixture;
import dev.confluxlab.experience.FamilyController;
import dev.confluxlab.experience.fixture.ExperienceFixtures;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureRuleDetectionTest {

    @Test
    void detectsACycleBetweenCapabilityAndExperience() {
        assertRejected(ModuleArchitectureRules.MODULES_HAVE_NO_CYCLES,
                CapabilityFixtures.InCycle.class, ExperienceFixtures.InCycle.class);
    }

    @Test
    void detectsCapabilityDependingOnTheBff() {
        assertRejected(ModuleArchitectureRules.CAPABILITY_DOES_NOT_DEPEND_ON_EXPERIENCE,

                CapabilityFixtures.DependsOnBff.class, FamilyController.class);
    }

    @Test
    void detectsBffAccessToAnInternalRepository() {
        assertRejected(ModuleArchitectureRules.EXPERIENCE_ONLY_USES_PUBLIC_CAPABILITY_API,
                ExperienceFixtures.UsesRepository.class, CapabilityFixtures.Repository.class);
    }

    @Test
    void detectsBffAccessToAnInternalDocument() {
        assertRejected(ModuleArchitectureRules.EXPERIENCE_ONLY_USES_PUBLIC_CAPABILITY_API,
                ExperienceFixtures.UsesDocument.class, CapabilityFixtures.Document.class);
    }

    @Test
    void detectsContractsDependingOnCapability() {
        assertRejected(ModuleArchitectureRules.CONTRACTS_DO_NOT_DEPEND_ON_OTHER_MODULES,
                ContractsFixture.class, FamilyRequests.class);
    }

    @Test
    void detectsALibraryDependingOnTheRootBootEntryPoint() {
        assertRejected(ModuleArchitectureRules.LIBRARIES_DO_NOT_DEPEND_ON_APPLICATION,

                ExperienceFixtures.DependsOnApplication.class, ConfluxLabApplication.class);
    }

    @Test
    void acceptsThePublicJavaApiAndSharedContractDependencies() {
        JavaClasses classes = new ClassFileImporter().importClasses(
                ExperienceFixtures.UsesPublicApi.class, FamilyRequests.class,
                CreateFamilyRequest.class, PersonPayload.class, RequestAccepted.class,
                RequestView.class, RequestStatus.class);

        ModuleArchitectureRules.MODULES_HAVE_NO_CYCLES.check(classes);
        ModuleArchitectureRules.CAPABILITY_DOES_NOT_DEPEND_ON_EXPERIENCE.check(classes);
        ModuleArchitectureRules.EXPERIENCE_ONLY_USES_PUBLIC_CAPABILITY_API.check(classes);
        ModuleArchitectureRules.CONTRACTS_DO_NOT_DEPEND_ON_OTHER_MODULES.check(classes);
        ModuleArchitectureRules.LIBRARIES_DO_NOT_DEPEND_ON_APPLICATION.check(classes);
    }

    private static void assertRejected(ArchRule rule, Class<?> origin, Class<?> target) {
        JavaClasses classes = new ClassFileImporter().importClasses(origin, target);
        EvaluationResult result = rule.evaluate(classes);
        String report = result.getFailureReport().toString();

        assertTrue(result.hasViolation(), "The rule must reject the deliberately invalid fixture.");
        assertTrue(report.contains(origin.getName()), report);
        assertTrue(report.contains(target.getName()), report);
    }
}
