package dev.confluxlab;

import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideOutsideOfPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideOutsideOfPackages;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

final class ModuleArchitectureRules {

    private static final String ROOT = "dev.confluxlab";
    private static final String CONTRACTS = ROOT + ".contracts..";
    private static final String CAPABILITY = ROOT + ".capability..";
    private static final String CAPABILITY_API = ROOT + ".capability.api..";
    private static final String EXPERIENCE = ROOT + ".experience..";

    static final ArchRule MODULES_HAVE_NO_CYCLES = slices()
            .matching(ROOT + ".(*)..")
            .should().beFreeOfCycles();

    static final ArchRule CAPABILITY_DOES_NOT_DEPEND_ON_EXPERIENCE = noClasses()
            .that().resideInAPackage(CAPABILITY)
            .should().dependOnClassesThat().resideInAPackage(EXPERIENCE);

    static final ArchRule EXPERIENCE_ONLY_USES_PUBLIC_CAPABILITY_API = noClasses()
            .that().resideInAPackage(EXPERIENCE)
            .should().dependOnClassesThat(
                    resideInAPackage(CAPABILITY).and(resideOutsideOfPackage(CAPABILITY_API)));

    static final ArchRule CONTRACTS_DO_NOT_DEPEND_ON_OTHER_MODULES = noClasses()
            .that().resideInAPackage(CONTRACTS)
            .should().dependOnClassesThat(
                    resideInAPackage(ROOT + "..").and(resideOutsideOfPackage(CONTRACTS)));

    // The Boot entry point is in the root package, outside the slices captured above.
    static final ArchRule LIBRARIES_DO_NOT_DEPEND_ON_APPLICATION = noClasses()
            .that(resideInAPackage(ROOT + "..")
                    .and(resideOutsideOfPackages(ROOT, ROOT + ".application..")))
            .should().dependOnClassesThat().resideInAnyPackage(ROOT, ROOT + ".application..");

    private ModuleArchitectureRules() {
    }
}
