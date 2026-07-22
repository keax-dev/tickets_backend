package com.tickets.managementtickets.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.tickets.managementtickets.category.application.port.CategoryRepositoryPort;
import com.tickets.managementtickets.category.application.service.CategoryService;
import com.tickets.managementtickets.dashboard.application.service.DashboardService;
import com.tickets.managementtickets.identity.application.port.AccessTokenService;
import com.tickets.managementtickets.identity.application.port.AuthSecuritySettings;
import com.tickets.managementtickets.identity.application.port.CurrentAuthenticatedUserProvider;
import com.tickets.managementtickets.identity.application.port.RefreshTokenGenerator;
import com.tickets.managementtickets.identity.application.port.RefreshTokenRepositoryPort;
import com.tickets.managementtickets.identity.application.port.UserRepositoryPort;
import com.tickets.managementtickets.identity.application.service.AuthService;
import com.tickets.managementtickets.identity.application.service.AuthorizationService;
import com.tickets.managementtickets.identity.application.service.RolePermissionService;
import com.tickets.managementtickets.identity.application.service.UserManagementService;
import com.tickets.managementtickets.identity.infrastructure.security.SecurityProperties;
import com.tickets.managementtickets.notification.application.port.NotificationRepositoryPort;
import com.tickets.managementtickets.notification.application.service.NotificationService;
import com.tickets.managementtickets.shared.application.port.HashingService;
import com.tickets.managementtickets.shared.application.port.JsonCodec;
import com.tickets.managementtickets.shared.application.port.PasswordHashingService;
import com.tickets.managementtickets.shared.application.port.TransactionRunner;
import com.tickets.managementtickets.sla.application.port.SlaPolicyRepositoryPort;
import com.tickets.managementtickets.sla.application.service.SlaPolicyService;
import com.tickets.managementtickets.ticket.application.port.IdempotencyPolicy;
import com.tickets.managementtickets.ticket.application.port.IdempotencyRecordRepositoryPort;
import com.tickets.managementtickets.ticket.application.port.TicketCodeGenerator;
import com.tickets.managementtickets.ticket.application.port.TicketCommentRepositoryPort;
import com.tickets.managementtickets.ticket.application.port.TicketHistoryRepositoryPort;
import com.tickets.managementtickets.ticket.application.port.TicketLifecyclePolicy;
import com.tickets.managementtickets.ticket.application.port.TicketRepositoryPort;
import com.tickets.managementtickets.ticket.application.service.TicketService;
import com.tickets.managementtickets.ticket.infrastructure.support.IdempotencyProperties;
import com.tickets.managementtickets.ticket.infrastructure.support.TicketLifecycleProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties({
    SecurityProperties.class,
    IdempotencyProperties.class,
    TicketLifecycleProperties.class
})
public class ApplicationConfiguration {

    @Bean
    Clock systemClock() {
        return Clock.systemUTC();
    }

    @Bean
    ObjectMapper objectMapper() {
        return JsonMapper.builder()
            .findAndAddModules()
            .build();
    }

    @Bean
    RolePermissionService rolePermissionService() {
        return new RolePermissionService();
    }

    @Bean
    AuthorizationService authorizationService() {
        return new AuthorizationService();
    }

    @Bean
    AuthService authService(
        UserRepositoryPort userRepository,
        RefreshTokenRepositoryPort refreshTokenRepository,
        PasswordHashingService passwordHashingService,
        RolePermissionService rolePermissionService,
        AccessTokenService accessTokenService,
        RefreshTokenGenerator refreshTokenGenerator,
        AuthSecuritySettings securitySettings,
        HashingService hashingService,
        CurrentAuthenticatedUserProvider currentUserProvider,
        Clock clock,
        TransactionRunner transactionRunner
    ) {
        return new AuthService(
            userRepository,
            refreshTokenRepository,
            passwordHashingService,
            rolePermissionService,
            accessTokenService,
            refreshTokenGenerator,
            securitySettings,
            hashingService,
            currentUserProvider,
            clock,
            transactionRunner
        );
    }

    @Bean
    UserManagementService userManagementService(
        UserRepositoryPort userRepository,
        AuthorizationService authorizationService,
        PasswordHashingService passwordHashingService,
        TransactionRunner transactionRunner
    ) {
        return new UserManagementService(userRepository, authorizationService, passwordHashingService, transactionRunner);
    }

    @Bean
    CategoryService categoryService(
        CategoryRepositoryPort categoryRepository,
        AuthorizationService authorizationService,
        TransactionRunner transactionRunner
    ) {
        return new CategoryService(categoryRepository, authorizationService, transactionRunner);
    }

    @Bean
    SlaPolicyService slaPolicyService(
        SlaPolicyRepositoryPort slaPolicyRepository,
        AuthorizationService authorizationService,
        TransactionRunner transactionRunner
    ) {
        return new SlaPolicyService(slaPolicyRepository, authorizationService, transactionRunner);
    }

    @Bean
    NotificationService notificationService(
        NotificationRepositoryPort notificationRepository,
        AuthorizationService authorizationService,
        Clock clock,
        TransactionRunner transactionRunner
    ) {
        return new NotificationService(notificationRepository, authorizationService, clock, transactionRunner);
    }

    @Bean
    DashboardService dashboardService(
        TicketRepositoryPort ticketRepository,
        TicketHistoryRepositoryPort ticketHistoryRepository,
        UserRepositoryPort userRepository,
        AuthorizationService authorizationService,
        Clock clock,
        TransactionRunner transactionRunner
    ) {
        return new DashboardService(ticketRepository, ticketHistoryRepository, userRepository, authorizationService, clock, transactionRunner);
    }

    @Bean
    TicketService ticketService(
        TicketRepositoryPort ticketRepository,
        TicketCommentRepositoryPort ticketCommentRepository,
        TicketHistoryRepositoryPort ticketHistoryRepository,
        IdempotencyRecordRepositoryPort idempotencyRecordRepository,
        UserRepositoryPort userRepository,
        CategoryRepositoryPort categoryRepository,
        SlaPolicyRepositoryPort slaPolicyRepository,
        NotificationRepositoryPort notificationRepository,
        TicketCodeGenerator ticketCodeGenerator,
        AuthorizationService authorizationService,
        HashingService hashingService,
        JsonCodec jsonCodec,
        Clock clock,
        IdempotencyPolicy idempotencyPolicy,
        TicketLifecyclePolicy ticketLifecyclePolicy,
        TransactionRunner transactionRunner
    ) {
        return new TicketService(
            ticketRepository,
            ticketCommentRepository,
            ticketHistoryRepository,
            idempotencyRecordRepository,
            userRepository,
            categoryRepository,
            slaPolicyRepository,
            notificationRepository,
            ticketCodeGenerator,
            authorizationService,
            hashingService,
            jsonCodec,
            clock,
            idempotencyPolicy,
            ticketLifecyclePolicy,
            transactionRunner
        );
    }
}
