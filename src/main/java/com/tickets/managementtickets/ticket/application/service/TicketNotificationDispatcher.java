package com.tickets.managementtickets.ticket.application.service;

import com.tickets.managementtickets.identity.application.port.UserRepositoryPort;
import com.tickets.managementtickets.identity.domain.model.Role;
import com.tickets.managementtickets.identity.domain.model.User;
import com.tickets.managementtickets.notification.application.port.NotificationRepositoryPort;
import com.tickets.managementtickets.notification.domain.model.Notification;
import com.tickets.managementtickets.notification.domain.model.NotificationType;
import com.tickets.managementtickets.ticket.domain.model.Ticket;

import java.util.List;

final class TicketNotificationDispatcher {

    private final NotificationRepositoryPort notificationRepository;
    private final UserRepositoryPort userRepository;

    TicketNotificationDispatcher(NotificationRepositoryPort notificationRepository, UserRepositoryPort userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    void ticketCreated(Ticket ticket) {
        notifySupportUsers(
            NotificationType.TICKET_CREATED,
            "Nuevo ticket creado",
            "Se creo el ticket " + ticket.getCode() + ".",
            ticket.getId()
        );
    }

    void ticketAssigned(Ticket ticket, String assigneeId) {
        notifyUser(
            assigneeId,
            NotificationType.TICKET_ASSIGNED,
            "Ticket asignado",
            "Se te asigno el ticket " + ticket.getCode() + ".",
            ticket.getId()
        );
    }

    void informationRequested(Ticket ticket) {
        notifyUser(
            ticket.getRequesterId(),
            NotificationType.INFORMATION_REQUESTED,
            "Se requiere informacion",
            "Hay una solicitud de informacion en el ticket " + ticket.getCode() + ".",
            ticket.getId()
        );
    }

    void ticketResolved(Ticket ticket) {
        notifyUser(
            ticket.getRequesterId(),
            NotificationType.TICKET_RESOLVED,
            "Ticket resuelto",
            "El ticket " + ticket.getCode() + " fue resuelto.",
            ticket.getId()
        );
    }

    void ticketClosed(Ticket ticket) {
        notifyUser(
            ticket.getRequesterId(),
            NotificationType.TICKET_CLOSED,
            "Ticket cerrado",
            "El ticket " + ticket.getCode() + " fue cerrado.",
            ticket.getId()
        );
    }

    void ticketReopened(Ticket ticket) {
        if (ticket.getAssignedAgentId() == null) {
            return;
        }
        notifyUser(
            ticket.getAssignedAgentId(),
            NotificationType.TICKET_REOPENED,
            "Ticket reabierto",
            "El ticket " + ticket.getCode() + " fue reabierto.",
            ticket.getId()
        );
    }

    void publicCommentAddedForRequester(Ticket ticket) {
        if (ticket.getRequesterId() == null) {
            return;
        }
        notifyUser(
            ticket.getRequesterId(),
            NotificationType.PUBLIC_COMMENT_ADDED,
            "Nuevo comentario",
            "Hay un nuevo comentario en el ticket " + ticket.getCode() + ".",
            ticket.getId()
        );
    }

    void customerReplied(Ticket ticket) {
        if (ticket.getAssignedAgentId() == null) {
            return;
        }
        notifyUser(
            ticket.getAssignedAgentId(),
            NotificationType.PUBLIC_COMMENT_ADDED,
            "Respuesta del cliente",
            "El cliente respondio en el ticket " + ticket.getCode() + ".",
            ticket.getId()
        );
    }

    void ticketAutoClosed(Ticket ticket) {
        notifyUser(
            ticket.getRequesterId(),
            NotificationType.TICKET_CLOSED,
            "Ticket cerrado automaticamente",
            "El ticket " + ticket.getCode() + " fue cerrado automaticamente por inactividad.",
            ticket.getId()
        );
    }

    private void notifySupportUsers(NotificationType type, String title, String message, String ticketId) {
        List<User> recipients = userRepository.findAllByRoleInAndActiveTrue(List.of(Role.ADMIN, Role.SUPPORT_MANAGER));
        recipients.forEach(user -> notifyUser(user.id(), type, title, message, ticketId));
    }

    private void notifyUser(String recipientId, NotificationType type, String title, String message, String ticketId) {
        notificationRepository.save(Notification.create(recipientId, type, title, message, ticketId));
    }
}
