package dev.confluxlab;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class ModuleArchitectureTest {

    private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("dev.confluxlab");

    @Test
    void modulesHaveNoCycles() {
        ModuleArchitectureRules.MODULES_HAVE_NO_CYCLES.check(PRODUCTION_CLASSES);
    }

    @Test
    void capabilityDoesNotDependOnExperience() {
        ModuleArchitectureRules.CAPABILITY_DOES_NOT_DEPEND_ON_EXPERIENCE.check(PRODUCTION_CLASSES);
    }

    @Test
    void experienceOnlyUsesThePublicCapabilityApi() {
        ModuleArchitectureRules.EXPERIENCE_ONLY_USES_PUBLIC_CAPABILITY_API.check(PRODUCTION_CLASSES);
    }

    @Test
    void contractsDoNotDependOnOtherModules() {
        ModuleArchitectureRules.CONTRACTS_DO_NOT_DEPEND_ON_OTHER_MODULES.check(PRODUCTION_CLASSES);
    }

    @Test
    void librariesDoNotDependOnTheApplicationAssembly() {
        ModuleArchitectureRules.LIBRARIES_DO_NOT_DEPEND_ON_APPLICATION.check(PRODUCTION_CLASSES);
    }
}
