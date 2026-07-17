package com.tickets.managementtickets.ticket.infrastructure.persistence.entity;

import com.tickets.managementtickets.shared.infrastructure.persistence.BaseUuidEntity;
import com.tickets.managementtickets.ticket.domain.model.TicketPriority;
import com.tickets.managementtickets.ticket.domain.model.TicketStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "tickets")
public class TicketEntity extends BaseUuidEntity {

    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "description", nullable = false, length = 5000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TicketStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 16)
    private TicketPriority priority;

    @Column(name = "requester_id", nullable = false, length = 36)
    private String requesterId;

    @Column(name = "assigned_agent_id", length = 36)
    private String assignedAgentId;

    @Column(name = "category_id", nullable = false, length = 36)
    private String categoryId;

    @Column(name = "first_response_due_at", nullable = false)
    private Instant firstResponseDueAt;

    @Column(name = "resolution_due_at", nullable = false)
    private Instant resolutionDueAt;

    @Column(name = "first_responded_at")
    private Instant firstRespondedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "sla_paused_at")
    private Instant slaPausedAt;

    @Column(name = "accumulated_paused_seconds", nullable = false)
    private long accumulatedPausedSeconds;

    @Column(name = "sla_first_response_breached", nullable = false)
    private boolean slaFirstResponseBreached;

    @Column(name = "sla_resolution_breached", nullable = false)
    private boolean slaResolutionBreached;

    @Column(name = "resolution_summary", length = 5000)
    private String resolutionSummary;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public void setPriority(TicketPriority priority) {
        this.priority = priority;
    }

    public String getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(String requesterId) {
        this.requesterId = requesterId;
    }

    public String getAssignedAgentId() {
        return assignedAgentId;
    }

    public void setAssignedAgentId(String assignedAgentId) {
        this.assignedAgentId = assignedAgentId;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public Instant getFirstResponseDueAt() {
        return firstResponseDueAt;
    }

    public void setFirstResponseDueAt(Instant firstResponseDueAt) {
        this.firstResponseDueAt = firstResponseDueAt;
    }

    public Instant getResolutionDueAt() {
        return resolutionDueAt;
    }

    public void setResolutionDueAt(Instant resolutionDueAt) {
        this.resolutionDueAt = resolutionDueAt;
    }

    public Instant getFirstRespondedAt() {
        return firstRespondedAt;
    }

    public void setFirstRespondedAt(Instant firstRespondedAt) {
        this.firstRespondedAt = firstRespondedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public Instant getSlaPausedAt() {
        return slaPausedAt;
    }

    public void setSlaPausedAt(Instant slaPausedAt) {
        this.slaPausedAt = slaPausedAt;
    }

    public long getAccumulatedPausedSeconds() {
        return accumulatedPausedSeconds;
    }

    public void setAccumulatedPausedSeconds(long accumulatedPausedSeconds) {
        this.accumulatedPausedSeconds = accumulatedPausedSeconds;
    }

    public boolean isSlaFirstResponseBreached() {
        return slaFirstResponseBreached;
    }

    public void setSlaFirstResponseBreached(boolean slaFirstResponseBreached) {
        this.slaFirstResponseBreached = slaFirstResponseBreached;
    }

    public boolean isSlaResolutionBreached() {
        return slaResolutionBreached;
    }

    public void setSlaResolutionBreached(boolean slaResolutionBreached) {
        this.slaResolutionBreached = slaResolutionBreached;
    }

    public String getResolutionSummary() {
        return resolutionSummary;
    }

    public void setResolutionSummary(String resolutionSummary) {
        this.resolutionSummary = resolutionSummary;
    }
}
