ALTER TABLE users
    ADD COLUMN locked_until TIMESTAMP(6) NULL DEFAULT NULL AFTER last_login_at;

SET @base_now = UTC_TIMESTAMP();
SET @system_user = '00000000-0000-0000-0000-000000000000';

INSERT INTO users (
    id, first_name, last_name, email, password_hash, role, active,
    failed_login_attempts, last_login_at, locked_until, created_at, updated_at, version
)
VALUES (
    @system_user,
    'System',
    'Automation',
    'system@tickets.local',
    '$2a$10$aJtp.9jpt2kj5yl.e5y1leChOj6TxS8yNiXF.kEXSjgZM5RD8sIBq',
    'ADMIN',
    b'0',
    0,
    NULL,
    NULL,
    @base_now,
    @base_now,
    0
)
ON DUPLICATE KEY UPDATE
    first_name = VALUES(first_name),
    last_name = VALUES(last_name),
    active = b'0',
    locked_until = NULL,
    updated_at = VALUES(updated_at);
