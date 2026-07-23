package com.tickets.managementtickets.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

// ArchUnit scans the production packages of the application and ignores test classes,
// because these rules are meant to validate the architecture of the real implementation only.
@AnalyzeClasses(packages = "com.tickets.managementtickets", importOptions = ImportOption.DoNotIncludeTests.class)
// This test suite protects Clean Architecture boundaries across domain, application, web, and persistence layers.
class ArchitectureRulesTest {

    // Application layer rule: application code must stay independent from frameworks and infrastructure details.
    @ArchTest
    static final ArchRule application_should_not_depend_on_infrastructure_or_frameworks = noClasses()
        // Select every production class that lives inside any application package.
        .that()
        .resideInAPackage("..application..")
        // Assert that those classes do not depend on infrastructure or framework-specific types.
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "..infrastructure..",
            "org.springframework..",
            "jakarta.persistence..",
            "jakarta.servlet..",
            "com.fasterxml.."
        );

    // Web adapter rule: controllers must call application services instead of persistence repositories directly.
    @ArchTest
    static final ArchRule web_controllers_should_not_access_repositories_directly = noClasses()
        // Select classes that belong to the HTTP/web adapter.
        .that()
        .resideInAPackage("..infrastructure.web..")
        // Prevent those classes from jumping over the application layer into repository implementations.
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..infrastructure.persistence.repository..");

    // Application layer rule: application code must not depend on HTTP/web DTOs.
    @ArchTest
    static final ArchRule application_should_not_depend_on_web_dtos = noClasses()
        // Select every class inside the application layer.
        .that()
        .resideInAPackage("..application..")
        // Application services and models must stay unaware of transport-specific DTOs.
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..infrastructure.web.dto..");

    // Application service rule: use explicit composition instead of Spring stereotypes in application services.
    @ArchTest
    static final ArchRule application_services_should_not_be_spring_services = noClasses()
        // Select only application service classes.
        .that()
        .resideInAPackage("..application.service..")
        // Require them to remain plain Java classes instead of Spring-managed stereotypes.
        .should()
        .beAnnotatedWith(Service.class);

    // Application service rule: keep application services free from generic Spring component annotations.
    @ArchTest
    static final ArchRule application_services_should_not_be_spring_components = noClasses()
        // Select only application service classes again.
        .that()
        .resideInAPackage("..application.service..")
        // This also blocks broader Spring component stereotypes that would couple the layer to the framework.
        .should()
        .beAnnotatedWith(Component.class);

    // Infrastructure rule: the web layer must not depend on persistence implementation details.
    @ArchTest
    static final ArchRule web_layer_should_not_depend_on_persistence_layer = noClasses()
        // Select classes from the web adapter.
        .that()
        .resideInAPackage("..infrastructure.web..")
        // The web layer should talk to application services, not to persistence entities or adapters.
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..infrastructure.persistence..");

    // Infrastructure rule: persistence adapters must not depend on HTTP/web adapter details.
    @ArchTest
    static final ArchRule persistence_layer_should_not_depend_on_web_layer = noClasses()
        // Select classes from persistence implementations.
        .that()
        .resideInAPackage("..infrastructure.persistence..")
        // Persistence must stay isolated from controller and DTO concerns.
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..infrastructure.web..");

    // Web DTO rule: request/response DTOs should remain mapping structures, not application-service clients.
    @ArchTest
    static final ArchRule web_dtos_should_not_depend_on_application_services = noClasses()
        // Select transport DTOs exposed by the web adapter.
        .that()
        .resideInAPackage("..infrastructure.web.dto..")
        // DTOs should only carry data, so they must not invoke or know application services directly.
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..application.service..");

    // Domain layer rule: domain code must stay pure and independent from application, infrastructure, and frameworks.
    @ArchTest
    static final ArchRule domain_should_not_depend_on_application_infrastructure_or_frameworks = noClasses()
        // Select every class in the domain layer.
        .that()
        .resideInAPackage("..domain..")
        // The domain model should not know about use cases, adapters, Spring, Jakarta, or JSON libraries.
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "..application..",
            "..infrastructure..",
            "org.springframework..",
            "jakarta..",
            "com.fasterxml.."
        );

    // Persistence entity rule: JPA entities must not know about application layer types.
    @ArchTest
    static final ArchRule persistence_entities_should_not_depend_on_application = noClasses()
        // Select JPA entity classes only.
        .that()
        .resideInAPackage("..infrastructure.persistence.entity..")
        // Persistence entities should map database state, not reference use-case models or services.
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..application..");

    // Module rule: bounded contexts/modules must not form package dependency cycles.
    @ArchTest
    static final ArchRule modules_should_not_form_dependency_cycles = slices()
        // Treat each first-level module below the root package as an architectural slice.
        .matching("com.tickets.managementtickets.(*)..")
        // Enforce an acyclic dependency graph so modules remain easier to reason about and refactor.
        .should()
        .beFreeOfCycles();
}
