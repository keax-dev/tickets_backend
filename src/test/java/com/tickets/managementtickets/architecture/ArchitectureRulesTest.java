package com.tickets.managementtickets.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packages = "com.tickets.managementtickets", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureRulesTest {

    @ArchTest
    static final ArchRule application_should_not_depend_on_infrastructure_or_frameworks = noClasses()
        .that()
        .resideInAPackage("..application..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "..infrastructure..",
            "org.springframework..",
            "jakarta.persistence..",
            "jakarta.servlet..",
            "com.fasterxml.."
        );

    @ArchTest
    static final ArchRule web_controllers_should_not_access_repositories_directly = noClasses()
        .that()
        .resideInAPackage("..infrastructure.web..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..infrastructure.persistence.repository..");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_application_infrastructure_or_frameworks = noClasses()
        .that()
        .resideInAPackage("..domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "..application..",
            "..infrastructure..",
            "org.springframework..",
            "jakarta..",
            "com.fasterxml.."
        );

    @ArchTest
    static final ArchRule persistence_entities_should_not_depend_on_application = noClasses()
        .that()
        .resideInAPackage("..infrastructure.persistence.entity..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..application..");

    @ArchTest
    static final ArchRule modules_should_not_form_dependency_cycles = slices()
        .matching("com.tickets.managementtickets.(*)..")
        .should()
        .beFreeOfCycles();
}
