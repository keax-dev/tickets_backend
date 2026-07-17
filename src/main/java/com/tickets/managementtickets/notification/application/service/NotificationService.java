package com.tickets.managementtickets.notification.application.service;

import com.tickets.managementtickets.identity.application.model.AuthenticatedUser;
import com.tickets.managementtickets.identity.application.service.AuthorizationService;
import com.tickets.managementtickets.identity.domain.model.Permission;
import com.tickets.managementtickets.notification.infrastructure.persistence.entity.NotificationEntity;
import com.tickets.managementtickets.notification.infrastructure.persistence.repository.NotificationRepository;
import com.tickets.managementtickets.shared.application.exception.NotFoundException;
import com.tickets.managementtickets.shared.application.model.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final AuthorizationService authorizationService;
    private final Clock clock;

    public NotificationService(
        NotificationRepository notificationRepository,
        AuthorizationService authorizationService,
        Clock clock
    ) {
        this.notificationRepository = notificationRepository;
        this.authorizationService = authorizationService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(AuthenticatedUser currentUser, int page, int size) {
        authorizationService.requirePermission(currentUser, Permission.NOTIFICATION_READ);
        return PageResponse.fromPage(
            notificationRepository.findAllByRecipientIdOrderByCreatedAtDesc(currentUser.id(), PageRequest.of(page, size))
                .map(this::toResponse)
        );
    }

    @Transactional
    public NotificationResponse markAsRead(AuthenticatedUser currentUser, String notificationId) {
        authorizationService.requirePermission(currentUser, Permission.NOTIFICATION_READ);
        NotificationEntity notification = notificationRepository.findByIdAndRecipientId(notificationId, currentUser.id())
            .orElseThrow(() -> new NotFoundException("NOTIFICATION_NOT_FOUND", "The notification could not be found."));

        notification.setRead(true);
        notification.setReadAt(clock.instant());
        return toResponse(notification);
    }

    @Transactional
    public void markAllAsRead(AuthenticatedUser currentUser) {
        authorizationService.requirePermission(currentUser, Permission.NOTIFICATION_READ);
        notificationRepository.findAllByRecipientIdAndReadFalse(currentUser.id())
            .forEach(notification -> {
                notification.setRead(true);
                notification.setReadAt(clock.instant());
            });
    }

    private NotificationResponse toResponse(NotificationEntity notification) {
        return new NotificationResponse(
            notification.getId(),
            notification.getType(),
            notification.getTitle(),
            notification.getMessage(),
            notification.getRelatedTicketId(),
            notification.isRead(),
            notification.getCreatedAt(),
            notification.getReadAt()
        );
    }

    public record NotificationResponse(
        String id,
        com.tickets.managementtickets.notification.domain.model.NotificationType type,
        String title,
        String message,
        String relatedTicketId,
        boolean read,
        java.time.Instant createdAt,
        java.time.Instant readAt
    ) {
    }
}
