package com.tickets.managementtickets.notification.application.service;

import com.tickets.managementtickets.identity.application.model.AuthenticatedUser;
import com.tickets.managementtickets.identity.application.service.AuthorizationService;
import com.tickets.managementtickets.identity.domain.model.Permission;
import com.tickets.managementtickets.notification.application.port.NotificationRepositoryPort;
import com.tickets.managementtickets.notification.application.result.NotificationResponse;
import com.tickets.managementtickets.notification.domain.model.Notification;
import com.tickets.managementtickets.shared.application.exception.NotFoundException;
import com.tickets.managementtickets.shared.application.model.PageResponse;
import com.tickets.managementtickets.shared.application.port.TransactionRunner;

import java.time.Clock;

public class NotificationService {

    private final NotificationRepositoryPort notificationRepository;
    private final AuthorizationService authorizationService;
    private final Clock clock;
    private final TransactionRunner transactionRunner;
    private final NotificationResponseMapper responseMapper;

    public NotificationService(
        NotificationRepositoryPort notificationRepository,
        AuthorizationService authorizationService,
        Clock clock,
        TransactionRunner transactionRunner
    ) {
        this.notificationRepository = notificationRepository;
        this.authorizationService = authorizationService;
        this.clock = clock;
        this.transactionRunner = transactionRunner;
        this.responseMapper = new NotificationResponseMapper();
    }

    public PageResponse<NotificationResponse> list(AuthenticatedUser currentUser, int page, int size) {
        return transactionRunner.readOnly(() -> {
            authorizationService.requirePermission(currentUser, Permission.NOTIFICATION_READ);
            return notificationRepository
                .findAllByRecipientIdOrderByCreatedAtDesc(currentUser.id(), page, size)
                .map(responseMapper::toResponse);
        });
    }

    public NotificationResponse markAsRead(AuthenticatedUser currentUser, String notificationId) {
        return transactionRunner.required(() -> {
            authorizationService.requirePermission(currentUser, Permission.NOTIFICATION_READ);
            Notification notification = notificationRepository.findByIdAndRecipientId(notificationId, currentUser.id())
                .orElseThrow(() -> new NotFoundException("NOTIFICATION_NOT_FOUND", "The notification could not be found."));

            return responseMapper.toResponse(notificationRepository.save(notification.markAsRead(clock.instant())));
        });
    }

    public void markAllAsRead(AuthenticatedUser currentUser) {
        transactionRunner.required(() -> {
            authorizationService.requirePermission(currentUser, Permission.NOTIFICATION_READ);
            notificationRepository.findAllByRecipientIdAndReadFalse(currentUser.id())
                .forEach(notification -> notificationRepository.save(notification.markAsRead(clock.instant())));
        });
    }

}
