package com.tickets.managementtickets.ticket.application.port;

import com.tickets.managementtickets.ticket.domain.model.CommentVisibility;
import com.tickets.managementtickets.ticket.domain.model.Ticket;

public interface TicketMetricsPort {

    TicketMetricsPort NO_OP = new TicketMetricsPort() {
        @Override
        public void recordTicketCreated(Ticket ticket) {
        }

        @Override
        public void recordTicketResolved(Ticket ticket) {
        }

        @Override
        public void recordTicketClosed(Ticket ticket, boolean automatic) {
        }

        @Override
        public void recordTicketCancelled(Ticket ticket) {
        }

        @Override
        public void recordCommentAdded(CommentVisibility visibility) {
        }
    };

    void recordTicketCreated(Ticket ticket);

    void recordTicketResolved(Ticket ticket);

    void recordTicketClosed(Ticket ticket, boolean automatic);

    void recordTicketCancelled(Ticket ticket);

    void recordCommentAdded(CommentVisibility visibility);
}
