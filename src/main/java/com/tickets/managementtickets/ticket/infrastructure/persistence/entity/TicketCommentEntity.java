package com.tickets.managementtickets.ticket.infrastructure.persistence.entity;

import com.tickets.managementtickets.shared.infrastructure.persistence.BaseUuidEntity;
import com.tickets.managementtickets.ticket.domain.model.CommentVisibility;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "ticket_comments")
public class TicketCommentEntity extends BaseUuidEntity {

    @Column(name = "ticket_id", nullable = false, length = 36)
    private String ticketId;

    @Column(name = "author_id", nullable = false, length = 36)
    private String authorId;

    @Column(name = "content", nullable = false, length = 4000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 16)
    private CommentVisibility visibility;

    public String getTicketId() {
        return ticketId;
    }

    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public CommentVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(CommentVisibility visibility) {
        this.visibility = visibility;
    }
}
