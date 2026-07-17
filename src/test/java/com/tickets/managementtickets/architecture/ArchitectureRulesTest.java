package com.tickets.managementtickets.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.tickets.managementtickets")
class ArchitectureRulesTest {

    @ArchTest
    static final ArchRule application_should_not_depend_on_web_or_security_adapters = noClasses()
        .that()
        .resideInAPackage("..application..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "..infrastructure.web..",
            "..infrastructure.security..",
            "..shared.infrastructure.web..",
            "..shared.infrastructure.security.."
        );

    @ArchTest
    static final ArchRule web_controllers_should_not_access_repositories_directly = noClasses()
        .that()
        .resideInAPackage("..infrastructure.web..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..infrastructure.persistence.repository..");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_spring_or_jpa = noClasses()
        .that()
        .resideInAPackage("..domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("org.springframework..", "jakarta.persistence..");
}
