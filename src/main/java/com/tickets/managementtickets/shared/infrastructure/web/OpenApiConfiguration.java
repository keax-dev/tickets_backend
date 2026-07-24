package com.tickets.managementtickets.shared.infrastructure.web;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Management Tickets API",
        version = "v1",
        description = "REST API for ticket, SLA, user, notification, category, and dashboard management.",
        contact = @Contact(name = "Management Tickets Team"),
        license = @io.swagger.v3.oas.annotations.info.License(name = "Internal Use")
    )
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    in = SecuritySchemeIn.HEADER,
    description = "JWT access token sent in the Authorization header using the Bearer scheme."
)
public class OpenApiConfiguration {

    @Bean
    OpenAPI managementTicketsOpenApi() {
        return new OpenAPI()
            .info(new io.swagger.v3.oas.models.info.Info()
                .title("Management Tickets API")
                .version("v1")
                .description("REST API for managing tickets, users, authentication, notifications, categories, dashboard summaries, and SLA policies.")
                .license(new License().name("Internal Use")));
    }
}
