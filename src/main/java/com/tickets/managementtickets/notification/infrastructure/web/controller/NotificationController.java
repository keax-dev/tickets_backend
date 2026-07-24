package com.tickets.managementtickets.notification.infrastructure.web.controller;

import com.tickets.managementtickets.identity.application.port.CurrentAuthenticatedUserProvider;
import com.tickets.managementtickets.notification.application.service.NotificationService;
import com.tickets.managementtickets.notification.infrastructure.web.dto.NotificationResponse;
import com.tickets.managementtickets.shared.application.model.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "Notification inbox endpoints.")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentAuthenticatedUserProvider currentUserProvider;

    public NotificationController(NotificationService notificationService, CurrentAuthenticatedUserProvider currentUserProvider) {
        this.notificationService = notificationService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    @Operation(summary = "List notifications", description = "Returns the paged notification inbox for the authenticated user.")
    public PageResponse<NotificationResponse> list(
        @RequestParam(defaultValue = "0") @PositiveOrZero int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return notificationService
            .list(currentUserProvider.requireCurrentUser(), page, size)
            .map(NotificationResponse::from);
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "Mark one notification as read", description = "Marks a single notification as read for the authenticated recipient.")
    public NotificationResponse markAsRead(@PathVariable String notificationId) {
        return NotificationResponse.from(
            notificationService.markAsRead(currentUserProvider.requireCurrentUser(), notificationId)
        );
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark all notifications as read", description = "Marks every unread notification as read for the authenticated user.")
    public ResponseEntity<Void> markAllAsRead() {
        notificationService.markAllAsRead(currentUserProvider.requireCurrentUser());
        return ResponseEntity.noContent().build();
    }
}
