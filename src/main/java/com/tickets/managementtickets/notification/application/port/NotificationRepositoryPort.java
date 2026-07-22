package com.tickets.managementtickets.notification.application.port;

import com.tickets.managementtickets.notification.domain.model.Notification;
import com.tickets.managementtickets.shared.application.model.PageResponse;

import java.util.List;
import java.util.Optional;

public interface NotificationRepositoryPort {

    PageResponse<Notification> findAllByRecipientIdOrderByCreatedAtDesc(String recipientId, int page, int size);

    Optional<Notification> findByIdAndRecipientId(String id, String recipientId);

    List<Notification> findAllByRecipientIdAndReadFalse(String recipientId);

    Notification save(Notification notification);
}
