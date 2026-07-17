INSERT INTO categories (id, name, description, active, created_at, updated_at, version)
VALUES
    (UUID(), 'Access', 'Access and login related issues', b'1', UTC_TIMESTAMP(), UTC_TIMESTAMP(), 0),
    (UUID(), 'Hardware', 'Hardware issues and equipment requests', b'1', UTC_TIMESTAMP(), UTC_TIMESTAMP(), 0),
    (UUID(), 'Software', 'Software errors and installation requests', b'1', UTC_TIMESTAMP(), UTC_TIMESTAMP(), 0),
    (UUID(), 'Network', 'Network and connectivity incidents', b'1', UTC_TIMESTAMP(), UTC_TIMESTAMP(), 0),
    (UUID(), 'Service Request', 'General service requests', b'1', UTC_TIMESTAMP(), UTC_TIMESTAMP(), 0),
    (UUID(), 'Other', 'Other requests', b'1', UTC_TIMESTAMP(), UTC_TIMESTAMP(), 0);

INSERT INTO sla_policies (id, priority, first_response_hours, resolution_hours, active, created_at, updated_at, version)
VALUES
    (UUID(), 'LOW', 24, 72, b'1', UTC_TIMESTAMP(), UTC_TIMESTAMP(), 0),
    (UUID(), 'MEDIUM', 8, 48, b'1', UTC_TIMESTAMP(), UTC_TIMESTAMP(), 0),
    (UUID(), 'HIGH', 4, 24, b'1', UTC_TIMESTAMP(), UTC_TIMESTAMP(), 0),
    (UUID(), 'URGENT', 2, 8, b'1', UTC_TIMESTAMP(), UTC_TIMESTAMP(), 0);
