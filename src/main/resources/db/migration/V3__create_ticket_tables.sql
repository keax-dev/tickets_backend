CREATE TABLE ticket_sequences (
    sequence_year INT NOT NULL PRIMARY KEY,
    current_value BIGINT NOT NULL
);

CREATE TABLE tickets (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    code VARCHAR(20) NOT NULL,
    title VARCHAR(150) NOT NULL,
    description VARCHAR(5000) NOT NULL,
    status VARCHAR(32) NOT NULL,
    priority VARCHAR(16) NOT NULL,
    requester_id VARCHAR(36) NOT NULL,
    assigned_agent_id VARCHAR(36) NULL,
    category_id VARCHAR(36) NOT NULL,
    first_response_due_at TIMESTAMP NOT NULL,
    resolution_due_at TIMESTAMP NOT NULL,
    first_responded_at TIMESTAMP NULL,
    resolved_at TIMESTAMP NULL,
    closed_at TIMESTAMP NULL,
    cancelled_at TIMESTAMP NULL,
    sla_paused_at TIMESTAMP NULL,
    accumulated_paused_seconds BIGINT NOT NULL DEFAULT 0,
    sla_first_response_breached BIT(1) NOT NULL DEFAULT b'0',
    sla_resolution_breached BIT(1) NOT NULL DEFAULT b'0',
    resolution_summary VARCHAR(5000) NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_tickets_code UNIQUE (code),
    CONSTRAINT fk_tickets_requester FOREIGN KEY (requester_id) REFERENCES users(id),
    CONSTRAINT fk_tickets_assigned_agent FOREIGN KEY (assigned_agent_id) REFERENCES users(id),
    CONSTRAINT fk_tickets_category FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE INDEX idx_tickets_status ON tickets(status);
CREATE INDEX idx_tickets_priority ON tickets(priority);
CREATE INDEX idx_tickets_requester ON tickets(requester_id);
CREATE INDEX idx_tickets_assigned_agent ON tickets(assigned_agent_id);
CREATE INDEX idx_tickets_category ON tickets(category_id);
CREATE INDEX idx_tickets_created_at ON tickets(created_at);

CREATE TABLE ticket_comments (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    ticket_id VARCHAR(36) NOT NULL,
    author_id VARCHAR(36) NOT NULL,
    content VARCHAR(4000) NOT NULL,
    visibility VARCHAR(16) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_ticket_comments_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id),
    CONSTRAINT fk_ticket_comments_author FOREIGN KEY (author_id) REFERENCES users(id)
);

CREATE TABLE ticket_attachments (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    ticket_id VARCHAR(36) NOT NULL,
    comment_id VARCHAR(36) NULL,
    uploaded_by VARCHAR(36) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_ticket_attachments_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id),
    CONSTRAINT fk_ticket_attachments_comment FOREIGN KEY (comment_id) REFERENCES ticket_comments(id),
    CONSTRAINT fk_ticket_attachments_user FOREIGN KEY (uploaded_by) REFERENCES users(id)
);

CREATE TABLE ticket_history (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    ticket_id VARCHAR(36) NOT NULL,
    action VARCHAR(40) NOT NULL,
    performed_by VARCHAR(36) NOT NULL,
    previous_value VARCHAR(4000) NULL,
    new_value VARCHAR(4000) NULL,
    metadata_json VARCHAR(4000) NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_ticket_history_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id),
    CONSTRAINT fk_ticket_history_user FOREIGN KEY (performed_by) REFERENCES users(id)
);

CREATE INDEX idx_ticket_history_ticket_created_at ON ticket_history(ticket_id, created_at);

CREATE TABLE idempotency_records (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    idempotency_key VARCHAR(120) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    request_hash VARCHAR(128) NOT NULL,
    response_status INT NOT NULL,
    response_body TEXT NOT NULL,
    resource_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_idempotency_records_key_user UNIQUE (idempotency_key, user_id),
    CONSTRAINT fk_idempotency_records_user FOREIGN KEY (user_id) REFERENCES users(id)
);
