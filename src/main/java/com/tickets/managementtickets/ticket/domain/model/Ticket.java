package com.tickets.managementtickets.ticket.domain.model;

import com.tickets.managementtickets.sla.domain.model.SlaPolicy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class Ticket {

    private String id;
    private String code;
    private String title;
    private String description;
    private TicketStatus status;
    private TicketPriority priority;
    private String requesterId;
    private String assignedAgentId;
    private String categoryId;
    private Instant firstResponseDueAt;
    private Instant resolutionDueAt;
    private Instant firstRespondedAt;
    private Instant resolvedAt;
    private Instant closedAt;
    private Instant cancelledAt;
    private Instant slaPausedAt;
    private long accumulatedPausedSeconds;
    private boolean slaFirstResponseBreached;
    private boolean slaResolutionBreached;
    private String resolutionSummary;
    private Instant createdAt;
    private Instant updatedAt;
    private long version;

    public Ticket(
        String id,
        String code,
        String title,
        String description,
        TicketStatus status,
        TicketPriority priority,
        String requesterId,
        String assignedAgentId,
        String categoryId,
        Instant firstResponseDueAt,
        Instant resolutionDueAt,
        Instant firstRespondedAt,
        Instant resolvedAt,
        Instant closedAt,
        Instant cancelledAt,
        Instant slaPausedAt,
        long accumulatedPausedSeconds,
        boolean slaFirstResponseBreached,
        boolean slaResolutionBreached,
        String resolutionSummary,
        Instant createdAt,
        Instant updatedAt,
        long version
    ) {
        this.id = id;
        this.code = code;
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.requesterId = requesterId;
        this.assignedAgentId = assignedAgentId;
        this.categoryId = categoryId;
        this.firstResponseDueAt = firstResponseDueAt;
        this.resolutionDueAt = resolutionDueAt;
        this.firstRespondedAt = firstRespondedAt;
        this.resolvedAt = resolvedAt;
        this.closedAt = closedAt;
        this.cancelledAt = cancelledAt;
        this.slaPausedAt = slaPausedAt;
        this.accumulatedPausedSeconds = accumulatedPausedSeconds;
        this.slaFirstResponseBreached = slaFirstResponseBreached;
        this.slaResolutionBreached = slaResolutionBreached;
        this.resolutionSummary = resolutionSummary;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public static Ticket create(
        String code,
        String title,
        String description,
        TicketPriority priority,
        String requesterId,
        String categoryId,
        SlaPolicy slaPolicy,
        Instant now
    ) {
        return new Ticket(
            null,
            code,
            title,
            description,
            TicketStatus.CREATED,
            priority,
            requesterId,
            null,
            categoryId,
            now.plus(slaPolicy.firstResponseHours(), ChronoUnit.HOURS),
            now.plus(slaPolicy.resolutionHours(), ChronoUnit.HOURS),
            null,
            null,
            null,
            null,
            null,
            0,
            false,
            false,
            null,
            null,
            null,
            0
        );
    }

    public void updateDetails(String title, String description, String categoryId) {
        if (title != null && !title.isBlank()) {
            this.title = title;
        }
        if (description != null && !description.isBlank()) {
            this.description = description;
        }
        if (categoryId != null && !categoryId.isBlank()) {
            this.categoryId = categoryId;
        }
    }

    public void changePriority(TicketPriority priority, SlaPolicy slaPolicy, Instant fallbackStart) {
        if (priority == null || priority == this.priority) {
            return;
        }
        this.priority = priority;
        recalculateSlaForPriorityChange(slaPolicy, fallbackStart);
    }

    public void assign(String agentId) {
        this.assignedAgentId = agentId;
        if (status == TicketStatus.CREATED) {
            status = TicketStatus.ASSIGNED;
        }
    }

    public void start(Instant now) {
        status = TicketStatus.IN_PROGRESS;
        applyFirstResponseIfMissing(now);
    }

    public void requestInformation(Instant now) {
        status = TicketStatus.WAITING_FOR_CUSTOMER;
        pauseResolutionSla(now);
    }

    public void resolve(String resolutionSummary, Instant now) {
        resumeResolutionSla(now);
        applyFirstResponseIfMissing(now);
        status = TicketStatus.RESOLVED;
        this.resolutionSummary = resolutionSummary;
        resolvedAt = now;
        if (resolvedAt.isAfter(resolutionDueAt)) {
            slaResolutionBreached = true;
        }
    }

    public void close(Instant now) {
        status = TicketStatus.CLOSED;
        closedAt = now;
    }

    public void reopen(Instant now, SlaPolicy slaPolicy) {
        status = TicketStatus.IN_PROGRESS;
        resolvedAt = null;
        closedAt = null;
        cancelledAt = null;
        resolutionSummary = null;
        resolutionDueAt = now.plus(slaPolicy.resolutionHours(), ChronoUnit.HOURS);
        slaResolutionBreached = false;
        slaPausedAt = null;
    }

    public void cancel(Instant now) {
        status = TicketStatus.CANCELLED;
        cancelledAt = now;
    }

    public void continueAfterCustomerResponse(Instant now) {
        resumeResolutionSla(now);
        status = TicketStatus.IN_PROGRESS;
    }

    public void applyFirstResponseIfMissing(Instant now) {
        if (firstRespondedAt == null) {
            firstRespondedAt = now;
            if (now.isAfter(firstResponseDueAt)) {
                slaFirstResponseBreached = true;
            }
        }
    }

    public void pauseResolutionSla(Instant now) {
        if (slaPausedAt == null) {
            slaPausedAt = now;
        }
    }

    public void resumeResolutionSla(Instant now) {
        if (slaPausedAt != null) {
            long pausedSeconds = ChronoUnit.SECONDS.between(slaPausedAt, now);
            accumulatedPausedSeconds += pausedSeconds;
            resolutionDueAt = resolutionDueAt.plusSeconds(pausedSeconds);
            slaPausedAt = null;
        }
    }

    public boolean isTerminal() {
        return status == TicketStatus.CLOSED || status == TicketStatus.CANCELLED;
    }

    private void recalculateSlaForPriorityChange(SlaPolicy slaPolicy, Instant fallbackStart) {
        Instant slaStart = createdAt == null ? fallbackStart : createdAt;
        if (!slaFirstResponseBreached && firstRespondedAt == null) {
            firstResponseDueAt = slaStart.plus(slaPolicy.firstResponseHours(), ChronoUnit.HOURS);
        }
        if (!slaResolutionBreached && status != TicketStatus.RESOLVED && status != TicketStatus.CLOSED && status != TicketStatus.CANCELLED) {
            resolutionDueAt = slaStart
                .plus(slaPolicy.resolutionHours(), ChronoUnit.HOURS)
                .plusSeconds(accumulatedPausedSeconds);
        }
    }

    public String getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public String getRequesterId() {
        return requesterId;
    }

    public String getAssignedAgentId() {
        return assignedAgentId;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public Instant getFirstResponseDueAt() {
        return firstResponseDueAt;
    }

    public Instant getResolutionDueAt() {
        return resolutionDueAt;
    }

    public Instant getFirstRespondedAt() {
        return firstRespondedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public Instant getSlaPausedAt() {
        return slaPausedAt;
    }

    public long getAccumulatedPausedSeconds() {
        return accumulatedPausedSeconds;
    }

    public boolean isSlaFirstResponseBreached() {
        return slaFirstResponseBreached;
    }

    public boolean isSlaResolutionBreached() {
        return slaResolutionBreached;
    }

    public String getResolutionSummary() {
        return resolutionSummary;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }
}
