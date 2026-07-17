CREATE TABLE categories (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500) NULL,
    active BIT(1) NOT NULL DEFAULT b'1',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_categories_name UNIQUE (name)
);

CREATE TABLE sla_policies (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    priority VARCHAR(16) NOT NULL,
    first_response_hours INT NOT NULL,
    resolution_hours INT NOT NULL,
    active BIT(1) NOT NULL DEFAULT b'1',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_sla_policies_priority UNIQUE (priority)
);
