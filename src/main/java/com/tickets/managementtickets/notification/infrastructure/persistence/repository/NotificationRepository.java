package com.tickets.managementtickets.notification.infrastructure.persistence.repository;

import com.tickets.managementtickets.notification.infrastructure.persistence.entity.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<NotificationEntity, String> {

    Page<NotificationEntity> findAllByRecipientIdOrderByCreatedAtDesc(String recipientId, Pageable pageable);

    Optional<NotificationEntity> findByIdAndRecipientId(String id, String recipientId);

    List<NotificationEntity> findAllByRecipientIdAndReadFalse(String recipientId);
}
