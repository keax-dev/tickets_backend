CREATE TABLE notifications (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    recipient_id VARCHAR(36) NOT NULL,
    type VARCHAR(40) NOT NULL,
    title VARCHAR(160) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    related_ticket_id VARCHAR(36) NULL,
    is_read BIT(1) NOT NULL DEFAULT b'0',
    read_at TIMESTAMP(6) NULL DEFAULT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_notifications_user FOREIGN KEY (recipient_id) REFERENCES users(id),
    CONSTRAINT fk_notifications_ticket FOREIGN KEY (related_ticket_id) REFERENCES tickets(id)
);

CREATE INDEX idx_notifications_recipient_created_at ON notifications(recipient_id, created_at);
