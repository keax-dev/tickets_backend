-- Flyway seed migration with full demo dataset for Management Tickets.
-- This migration leaves the backend ready for end-to-end manual testing.
-- Shared password for every demo user in this file: Password123!
--
-- Suggested logins:
--   admin.demo@tickets.local
--   manager.demo@tickets.local
--   agent.alvarez@tickets.local
--   agent.nunez@tickets.local
--   agent.ortega@tickets.local
--   customer.finance@tickets.local
--   customer.operations@tickets.local
--   customer.sales@tickets.local
--   customer.hr@tickets.local
--   customer.executive@tickets.local

START TRANSACTION;

SET @base_now = UTC_TIMESTAMP();
SET @current_year = YEAR(@base_now);
SET @password_hash = '$2a$10$aJtp.9jpt2kj5yl.e5y1leChOj6TxS8yNiXF.kEXSjgZM5RD8sIBq';

SET @user_admin = '10000000-0000-0000-0000-000000000001';
SET @user_manager = '10000000-0000-0000-0000-000000000002';
SET @user_agent_ana = '10000000-0000-0000-0000-000000000003';
SET @user_agent_bruno = '10000000-0000-0000-0000-000000000004';
SET @user_agent_carla = '10000000-0000-0000-0000-000000000005';
SET @user_agent_inactive = '10000000-0000-0000-0000-000000000006';
SET @user_customer_finance = '10000000-0000-0000-0000-000000000011';
SET @user_customer_ops = '10000000-0000-0000-0000-000000000012';
SET @user_customer_sales = '10000000-0000-0000-0000-000000000013';
SET @user_customer_hr = '10000000-0000-0000-0000-000000000014';
SET @user_customer_exec = '10000000-0000-0000-0000-000000000015';
SET @user_customer_inactive = '10000000-0000-0000-0000-000000000016';

SET @ticket_01 = '30000000-0000-0000-0000-000000000001';
SET @ticket_02 = '30000000-0000-0000-0000-000000000002';
SET @ticket_03 = '30000000-0000-0000-0000-000000000003';
SET @ticket_04 = '30000000-0000-0000-0000-000000000004';
SET @ticket_05 = '30000000-0000-0000-0000-000000000005';
SET @ticket_06 = '30000000-0000-0000-0000-000000000006';
SET @ticket_07 = '30000000-0000-0000-0000-000000000007';
SET @ticket_08 = '30000000-0000-0000-0000-000000000008';
SET @ticket_09 = '30000000-0000-0000-0000-000000000009';
SET @ticket_10 = '30000000-0000-0000-0000-000000000010';
SET @ticket_11 = '30000000-0000-0000-0000-000000000011';
SET @ticket_12 = '30000000-0000-0000-0000-000000000012';
SET @ticket_13 = '30000000-0000-0000-0000-000000000013';
SET @ticket_14 = '30000000-0000-0000-0000-000000000014';
SET @ticket_15 = '30000000-0000-0000-0000-000000000015';
SET @ticket_16 = '30000000-0000-0000-0000-000000000016';

SET @code_ticket_01 = CONCAT('TCK-', @current_year, '-900001');
SET @code_ticket_02 = CONCAT('TCK-', @current_year, '-900002');
SET @code_ticket_03 = CONCAT('TCK-', @current_year, '-900003');
SET @code_ticket_04 = CONCAT('TCK-', @current_year, '-900004');
SET @code_ticket_05 = CONCAT('TCK-', @current_year, '-900005');
SET @code_ticket_06 = CONCAT('TCK-', @current_year, '-900006');
SET @code_ticket_07 = CONCAT('TCK-', @current_year, '-900007');
SET @code_ticket_08 = CONCAT('TCK-', @current_year, '-900008');
SET @code_ticket_09 = CONCAT('TCK-', @current_year, '-900009');
SET @code_ticket_10 = CONCAT('TCK-', @current_year, '-900010');
SET @code_ticket_11 = CONCAT('TCK-', @current_year, '-900011');
SET @code_ticket_12 = CONCAT('TCK-', @current_year, '-900012');
SET @code_ticket_13 = CONCAT('TCK-', @current_year, '-900013');
SET @code_ticket_14 = CONCAT('TCK-', @current_year, '-900014');
SET @code_ticket_15 = CONCAT('TCK-', @current_year, '-900015');
SET @code_ticket_16 = CONCAT('TCK-', @current_year, '-900016');

SET @ticket_01_created = DATE_SUB(@base_now, INTERVAL 45 MINUTE);
SET @ticket_02_created = DATE_SUB(@base_now, INTERVAL 27 HOUR);
SET @ticket_03_created = DATE_SUB(@base_now, INTERVAL 6 HOUR);
SET @ticket_04_created = DATE_SUB(@base_now, INTERVAL 6 HOUR);
SET @ticket_05_created = DATE_SUB(@base_now, INTERVAL 36 HOUR);
SET @ticket_06_created = DATE_SUB(@base_now, INTERVAL 168 HOUR);
SET @ticket_07_created = DATE_SUB(@base_now, INTERVAL 48 HOUR);
SET @ticket_08_created = DATE_SUB(@base_now, INTERVAL 48 HOUR);
SET @ticket_09_created = DATE_SUB(@base_now, INTERVAL 24 HOUR);
SET @ticket_10_created = DATE_SUB(@base_now, INTERVAL 120 HOUR);
SET @ticket_11_created = DATE_SUB(@base_now, INTERVAL 1 HOUR);
SET @ticket_12_created = DATE_SUB(@base_now, INTERVAL 12 HOUR);
SET @ticket_13_created = DATE_SUB(@base_now, INTERVAL 20 HOUR);
SET @ticket_14_created = DATE_SUB(@base_now, INTERVAL 3 HOUR);
SET @ticket_15_created = DATE_SUB(@base_now, INTERVAL 8 HOUR);
SET @ticket_16_created = DATE_SUB(@base_now, INTERVAL 9 HOUR);

DELETE FROM notifications
WHERE related_ticket_id IN (
    @ticket_01, @ticket_02, @ticket_03, @ticket_04,
    @ticket_05, @ticket_06, @ticket_07, @ticket_08,
    @ticket_09, @ticket_10, @ticket_11, @ticket_12,
    @ticket_13, @ticket_14, @ticket_15, @ticket_16
)
OR recipient_id IN (
    @user_admin, @user_manager, @user_agent_ana, @user_agent_bruno,
    @user_agent_carla, @user_agent_inactive, @user_customer_finance,
    @user_customer_ops, @user_customer_sales, @user_customer_hr,
    @user_customer_exec, @user_customer_inactive
);

DELETE FROM ticket_attachments
WHERE ticket_id IN (
    @ticket_01, @ticket_02, @ticket_03, @ticket_04,
    @ticket_05, @ticket_06, @ticket_07, @ticket_08,
    @ticket_09, @ticket_10, @ticket_11, @ticket_12,
    @ticket_13, @ticket_14, @ticket_15, @ticket_16
);

DELETE FROM ticket_comments
WHERE ticket_id IN (
    @ticket_01, @ticket_02, @ticket_03, @ticket_04,
    @ticket_05, @ticket_06, @ticket_07, @ticket_08,
    @ticket_09, @ticket_10, @ticket_11, @ticket_12,
    @ticket_13, @ticket_14, @ticket_15, @ticket_16
);

DELETE FROM ticket_history
WHERE ticket_id IN (
    @ticket_01, @ticket_02, @ticket_03, @ticket_04,
    @ticket_05, @ticket_06, @ticket_07, @ticket_08,
    @ticket_09, @ticket_10, @ticket_11, @ticket_12,
    @ticket_13, @ticket_14, @ticket_15, @ticket_16
);

DELETE FROM idempotency_records
WHERE resource_id IN (
    @ticket_01, @ticket_02, @ticket_03, @ticket_04,
    @ticket_05, @ticket_06, @ticket_07, @ticket_08,
    @ticket_09, @ticket_10, @ticket_11, @ticket_12,
    @ticket_13, @ticket_14, @ticket_15, @ticket_16
)
OR user_id IN (
    @user_admin, @user_manager, @user_agent_ana, @user_agent_bruno,
    @user_agent_carla, @user_agent_inactive, @user_customer_finance,
    @user_customer_ops, @user_customer_sales, @user_customer_hr,
    @user_customer_exec, @user_customer_inactive
);

DELETE FROM tickets
WHERE id IN (
    @ticket_01, @ticket_02, @ticket_03, @ticket_04,
    @ticket_05, @ticket_06, @ticket_07, @ticket_08,
    @ticket_09, @ticket_10, @ticket_11, @ticket_12,
    @ticket_13, @ticket_14, @ticket_15, @ticket_16
);

DELETE FROM refresh_tokens
WHERE user_id IN (
    @user_admin, @user_manager, @user_agent_ana, @user_agent_bruno,
    @user_agent_carla, @user_agent_inactive, @user_customer_finance,
    @user_customer_ops, @user_customer_sales, @user_customer_hr,
    @user_customer_exec, @user_customer_inactive
);

DELETE FROM users
WHERE id IN (
    @user_admin, @user_manager, @user_agent_ana, @user_agent_bruno,
    @user_agent_carla, @user_agent_inactive, @user_customer_finance,
    @user_customer_ops, @user_customer_sales, @user_customer_hr,
    @user_customer_exec, @user_customer_inactive
);

INSERT INTO categories (id, name, description, active, created_at, updated_at, version)
VALUES
    ('20000000-0000-0000-0000-000000000001', 'Access', 'Access and login related issues', b'1', @base_now, @base_now, 0),
    ('20000000-0000-0000-0000-000000000002', 'Hardware', 'Hardware issues and equipment requests', b'1', @base_now, @base_now, 0),
    ('20000000-0000-0000-0000-000000000003', 'Software', 'Software errors and installation requests', b'1', @base_now, @base_now, 0),
    ('20000000-0000-0000-0000-000000000004', 'Network', 'Network and connectivity incidents', b'1', @base_now, @base_now, 0),
    ('20000000-0000-0000-0000-000000000005', 'Service Request', 'General service requests', b'1', @base_now, @base_now, 0),
    ('20000000-0000-0000-0000-000000000006', 'Other', 'Other requests', b'1', @base_now, @base_now, 0),
    ('20000000-0000-0000-0000-000000000007', 'Security', 'Security incidents and privileged access requests', b'1', @base_now, @base_now, 0),
    ('20000000-0000-0000-0000-000000000008', 'Procurement', 'Requests for purchases, renewals and supplier coordination', b'1', @base_now, @base_now, 0),
    ('20000000-0000-0000-0000-000000000009', 'Legacy Systems', 'Historical applications pending retirement', b'0', @base_now, @base_now, 0)
ON DUPLICATE KEY UPDATE
    description = VALUES(description),
    active = VALUES(active),
    updated_at = VALUES(updated_at);

INSERT INTO sla_policies (id, priority, first_response_hours, resolution_hours, active, created_at, updated_at, version)
VALUES
    ('21000000-0000-0000-0000-000000000001', 'LOW', 24, 72, b'1', @base_now, @base_now, 0),
    ('21000000-0000-0000-0000-000000000002', 'MEDIUM', 8, 48, b'1', @base_now, @base_now, 0),
    ('21000000-0000-0000-0000-000000000003', 'HIGH', 4, 24, b'1', @base_now, @base_now, 0),
    ('21000000-0000-0000-0000-000000000004', 'URGENT', 2, 8, b'1', @base_now, @base_now, 0)
ON DUPLICATE KEY UPDATE
    first_response_hours = VALUES(first_response_hours),
    resolution_hours = VALUES(resolution_hours),
    active = VALUES(active),
    updated_at = VALUES(updated_at);

SET @category_access = (SELECT id FROM categories WHERE name = 'Access' LIMIT 1);
SET @category_hardware = (SELECT id FROM categories WHERE name = 'Hardware' LIMIT 1);
SET @category_software = (SELECT id FROM categories WHERE name = 'Software' LIMIT 1);
SET @category_network = (SELECT id FROM categories WHERE name = 'Network' LIMIT 1);
SET @category_service = (SELECT id FROM categories WHERE name = 'Service Request' LIMIT 1);
SET @category_other = (SELECT id FROM categories WHERE name = 'Other' LIMIT 1);
SET @category_security = (SELECT id FROM categories WHERE name = 'Security' LIMIT 1);
SET @category_procurement = (SELECT id FROM categories WHERE name = 'Procurement' LIMIT 1);

INSERT INTO users (
    id, first_name, last_name, email, password_hash, role, active,
    failed_login_attempts, last_login_at, created_at, updated_at, version
)
VALUES
    (@user_admin, 'Adrian', 'Demo', 'admin.demo@tickets.local', @password_hash, 'ADMIN', b'1', 0, DATE_SUB(@base_now, INTERVAL 25 MINUTE), DATE_SUB(@base_now, INTERVAL 60 DAY), DATE_SUB(@base_now, INTERVAL 25 MINUTE), 0),
    (@user_manager, 'Monica', 'Manager', 'manager.demo@tickets.local', @password_hash, 'SUPPORT_MANAGER', b'1', 0, DATE_SUB(@base_now, INTERVAL 15 MINUTE), DATE_SUB(@base_now, INTERVAL 55 DAY), DATE_SUB(@base_now, INTERVAL 15 MINUTE), 0),
    (@user_agent_ana, 'Ana', 'Alvarez', 'agent.alvarez@tickets.local', @password_hash, 'SUPPORT_AGENT', b'1', 0, DATE_SUB(@base_now, INTERVAL 55 MINUTE), DATE_SUB(@base_now, INTERVAL 40 DAY), DATE_SUB(@base_now, INTERVAL 55 MINUTE), 0),
    (@user_agent_bruno, 'Bruno', 'Nunez', 'agent.nunez@tickets.local', @password_hash, 'SUPPORT_AGENT', b'1', 0, DATE_SUB(@base_now, INTERVAL 1 HOUR), DATE_SUB(@base_now, INTERVAL 40 DAY), DATE_SUB(@base_now, INTERVAL 1 HOUR), 0),
    (@user_agent_carla, 'Carla', 'Ortega', 'agent.ortega@tickets.local', @password_hash, 'SUPPORT_AGENT', b'1', 0, DATE_SUB(@base_now, INTERVAL 2 HOUR), DATE_SUB(@base_now, INTERVAL 38 DAY), DATE_SUB(@base_now, INTERVAL 2 HOUR), 0),
    (@user_agent_inactive, 'Diego', 'Inactive', 'agent.inactive@tickets.local', @password_hash, 'SUPPORT_AGENT', b'0', 2, DATE_SUB(@base_now, INTERVAL 21 DAY), DATE_SUB(@base_now, INTERVAL 35 DAY), DATE_SUB(@base_now, INTERVAL 5 DAY), 0),
    (@user_customer_finance, 'Fabian', 'Finance', 'customer.finance@tickets.local', @password_hash, 'CUSTOMER', b'1', 0, DATE_SUB(@base_now, INTERVAL 10 MINUTE), DATE_SUB(@base_now, INTERVAL 30 DAY), DATE_SUB(@base_now, INTERVAL 10 MINUTE), 0),
    (@user_customer_ops, 'Olivia', 'Operations', 'customer.operations@tickets.local', @password_hash, 'CUSTOMER', b'1', 0, DATE_SUB(@base_now, INTERVAL 40 MINUTE), DATE_SUB(@base_now, INTERVAL 30 DAY), DATE_SUB(@base_now, INTERVAL 40 MINUTE), 0),
    (@user_customer_sales, 'Santiago', 'Sales', 'customer.sales@tickets.local', @password_hash, 'CUSTOMER', b'1', 0, DATE_SUB(@base_now, INTERVAL 80 MINUTE), DATE_SUB(@base_now, INTERVAL 28 DAY), DATE_SUB(@base_now, INTERVAL 80 MINUTE), 0),
    (@user_customer_hr, 'Helena', 'HR', 'customer.hr@tickets.local', @password_hash, 'CUSTOMER', b'1', 0, DATE_SUB(@base_now, INTERVAL 5 HOUR), DATE_SUB(@base_now, INTERVAL 28 DAY), DATE_SUB(@base_now, INTERVAL 5 HOUR), 0),
    (@user_customer_exec, 'Valeria', 'Executive', 'customer.executive@tickets.local', @password_hash, 'CUSTOMER', b'1', 0, DATE_SUB(@base_now, INTERVAL 3 HOUR), DATE_SUB(@base_now, INTERVAL 26 DAY), DATE_SUB(@base_now, INTERVAL 3 HOUR), 0),
    (@user_customer_inactive, 'Irene', 'Former', 'customer.inactive@tickets.local', @password_hash, 'CUSTOMER', b'0', 1, DATE_SUB(@base_now, INTERVAL 32 DAY), DATE_SUB(@base_now, INTERVAL 26 DAY), DATE_SUB(@base_now, INTERVAL 14 DAY), 0);

INSERT INTO tickets (
    id, code, title, description, status, priority, requester_id, assigned_agent_id, category_id,
    first_response_due_at, resolution_due_at, first_responded_at, resolved_at, closed_at, cancelled_at,
    sla_paused_at, accumulated_paused_seconds, sla_first_response_breached, sla_resolution_breached,
    resolution_summary, created_at, updated_at, version
)
VALUES
    (@ticket_01, @code_ticket_01, 'VPN access for new analyst', 'Nuevo analista del area financiera necesita acceso VPN y validacion MFA.', 'CREATED', 'HIGH', @user_customer_finance, NULL, @category_access, DATE_ADD(@ticket_01_created, INTERVAL 4 HOUR), DATE_ADD(@ticket_01_created, INTERVAL 24 HOUR), NULL, NULL, NULL, NULL, NULL, 0, b'0', b'0', NULL, @ticket_01_created, @ticket_01_created, 0),
    (@ticket_02, @code_ticket_02, 'Replacement keyboard for warehouse station', 'El teclado de la estacion del almacon deja de registrar varias teclas.', 'ASSIGNED', 'LOW', @user_customer_ops, @user_agent_ana, @category_hardware, DATE_ADD(@ticket_02_created, INTERVAL 24 HOUR), DATE_ADD(@ticket_02_created, INTERVAL 72 HOUR), NULL, NULL, NULL, NULL, NULL, 0, b'0', b'0', NULL, @ticket_02_created, DATE_ADD(@ticket_02_created, INTERVAL 45 MINUTE), 1),
    (@ticket_03, @code_ticket_03, 'Accounting app crashes on export', 'Al exportar el libro mayor a Excel la aplicacion se cierra sin mensaje.', 'IN_PROGRESS', 'HIGH', @user_customer_sales, @user_agent_ana, @category_software, DATE_ADD(@ticket_03_created, INTERVAL 4 HOUR), DATE_ADD(@ticket_03_created, INTERVAL 24 HOUR), DATE_ADD(@ticket_03_created, INTERVAL 2 HOUR), NULL, NULL, NULL, NULL, 0, b'0', b'0', NULL, @ticket_03_created, DATE_ADD(@ticket_03_created, INTERVAL 4 HOUR), 3),
    (@ticket_04, @code_ticket_04, 'Intermittent WiFi on third floor', 'La red inalambrica pierde conectividad de forma aleatoria durante las reuniones.', 'WAITING_FOR_CUSTOMER', 'URGENT', @user_customer_finance, @user_agent_bruno, @category_network, DATE_ADD(@ticket_04_created, INTERVAL 2 HOUR), DATE_ADD(@ticket_04_created, INTERVAL 8 HOUR), DATE_ADD(@ticket_04_created, INTERVAL 1 HOUR), NULL, NULL, NULL, DATE_SUB(@base_now, INTERVAL 3 HOUR), 0, b'0', b'0', NULL, @ticket_04_created, DATE_SUB(@base_now, INTERVAL 3 HOUR), 4),
    (@ticket_05, @code_ticket_05, 'Office 365 distribution list update', 'Se necesita actualizar los integrantes de la lista de distribucion del area operativa.', 'RESOLVED', 'MEDIUM', @user_customer_ops, @user_agent_bruno, @category_service, DATE_ADD(@ticket_05_created, INTERVAL 8 HOUR), DATE_ADD(@ticket_05_created, INTERVAL 48 HOUR), DATE_ADD(@ticket_05_created, INTERVAL 2 HOUR), DATE_SUB(@base_now, INTERVAL 6 HOUR), NULL, NULL, NULL, 0, b'0', b'0', 'Lista actualizada y validada con el solicitante.', @ticket_05_created, DATE_SUB(@base_now, INTERVAL 6 HOUR), 4),
    (@ticket_06, @code_ticket_06, 'Laptop delivery confirmation', 'El usuario requiere confirmacion final del reemplazo de equipo entregado esta semana.', 'CLOSED', 'LOW', @user_customer_hr, @user_agent_carla, @category_other, DATE_ADD(@ticket_06_created, INTERVAL 24 HOUR), DATE_ADD(@ticket_06_created, INTERVAL 72 HOUR), DATE_ADD(@ticket_06_created, INTERVAL 6 HOUR), DATE_SUB(@base_now, INTERVAL 120 HOUR), DATE_SUB(@base_now, INTERVAL 116 HOUR), NULL, NULL, 0, b'0', b'0', 'Equipo entregado, configurado y aceptado por recursos humanos.', @ticket_06_created, DATE_SUB(@base_now, INTERVAL 116 HOUR), 5),
    (@ticket_07, @code_ticket_07, 'Duplicate onboarding request', 'Solicitud duplicada generada por error durante el alta del colaborador.', 'CANCELLED', 'LOW', @user_customer_sales, NULL, @category_service, DATE_ADD(@ticket_07_created, INTERVAL 24 HOUR), DATE_ADD(@ticket_07_created, INTERVAL 72 HOUR), NULL, NULL, NULL, DATE_SUB(@base_now, INTERVAL 47 HOUR), NULL, 0, b'0', b'0', NULL, @ticket_07_created, DATE_SUB(@base_now, INTERVAL 47 HOUR), 1),
    (@ticket_08, @code_ticket_08, 'Database credential reset delayed', 'La cuenta tecnica de base de datos no ha sido restablecida y afecta un despliegue.', 'IN_PROGRESS', 'HIGH', @user_customer_exec, @user_agent_carla, @category_security, DATE_ADD(@ticket_08_created, INTERVAL 4 HOUR), DATE_ADD(@ticket_08_created, INTERVAL 24 HOUR), DATE_ADD(@ticket_08_created, INTERVAL 6 HOUR), NULL, NULL, NULL, NULL, 0, b'1', b'1', NULL, @ticket_08_created, DATE_SUB(@base_now, INTERVAL 12 HOUR), 3),
    (@ticket_09, @code_ticket_09, 'Printer queue stuck for branch office', 'La cola de impresion de la sucursal remota se queda congelada despues de cada envio.', 'RESOLVED', 'URGENT', @user_customer_ops, @user_manager, @category_other, DATE_ADD(@ticket_09_created, INTERVAL 2 HOUR), DATE_ADD(@ticket_09_created, INTERVAL 8 HOUR), DATE_ADD(@ticket_09_created, INTERVAL 30 MINUTE), DATE_SUB(@base_now, INTERVAL 10 HOUR), NULL, NULL, NULL, 0, b'0', b'1', 'Se reinicio el spooler remoto y se liberaron los trabajos pendientes.', @ticket_09_created, DATE_SUB(@base_now, INTERVAL 10 HOUR), 4),
    (@ticket_10, @code_ticket_10, 'Quarter end report needs rerun', 'El reporte financiero de cierre fue reabierto porque faltaban asientos del ultimo lote.', 'IN_PROGRESS', 'MEDIUM', @user_customer_hr, @user_agent_bruno, @category_software, DATE_ADD(@ticket_10_created, INTERVAL 8 HOUR), DATE_ADD(@base_now, INTERVAL 30 HOUR), DATE_ADD(@ticket_10_created, INTERVAL 2 HOUR), NULL, NULL, NULL, NULL, 0, b'0', b'0', NULL, @ticket_10_created, DATE_SUB(@base_now, INTERVAL 18 HOUR), 5),
    (@ticket_11, @code_ticket_11, 'Production API latency spike', 'Se detecta latencia alta en el API productivo durante el horario de mayor carga.', 'ASSIGNED', 'URGENT', @user_customer_exec, @user_agent_carla, @category_network, DATE_ADD(@ticket_11_created, INTERVAL 2 HOUR), DATE_ADD(@ticket_11_created, INTERVAL 8 HOUR), NULL, NULL, NULL, NULL, NULL, 0, b'0', b'0', NULL, @ticket_11_created, DATE_ADD(@ticket_11_created, INTERVAL 10 MINUTE), 1),
    (@ticket_12, @code_ticket_12, 'Need proof of payment upload', 'El solicitante requiere subir comprobantes de pago en un caso administrativo.', 'WAITING_FOR_CUSTOMER', 'HIGH', @user_customer_ops, @user_agent_ana, @category_procurement, DATE_ADD(@ticket_12_created, INTERVAL 4 HOUR), DATE_ADD(@ticket_12_created, INTERVAL 24 HOUR), DATE_ADD(@ticket_12_created, INTERVAL 1 HOUR), NULL, NULL, NULL, DATE_SUB(@base_now, INTERVAL 4 HOUR), 0, b'0', b'0', NULL, @ticket_12_created, DATE_SUB(@base_now, INTERVAL 4 HOUR), 4),
    (@ticket_13, @code_ticket_13, 'Warehouse scanner firmware update', 'El escaner del almacon requiere actualizacion de firmware y validacion posterior.', 'IN_PROGRESS', 'MEDIUM', @user_customer_finance, @user_agent_bruno, @category_hardware, DATE_ADD(@ticket_13_created, INTERVAL 8 HOUR), DATE_ADD(@ticket_13_created, INTERVAL 50 HOUR), DATE_ADD(@ticket_13_created, INTERVAL 2 HOUR), NULL, NULL, NULL, NULL, 7200, b'0', b'0', NULL, @ticket_13_created, DATE_SUB(@base_now, INTERVAL 2 HOUR), 5),
    (@ticket_14, @code_ticket_14, 'Request access removed by mistake', 'El acceso fue retirado por error y se necesita restaurar antes del cierre del dia.', 'CREATED', 'MEDIUM', @user_customer_hr, NULL, @category_access, DATE_ADD(@ticket_14_created, INTERVAL 8 HOUR), DATE_ADD(@ticket_14_created, INTERVAL 48 HOUR), NULL, NULL, NULL, NULL, NULL, 0, b'0', b'0', NULL, @ticket_14_created, DATE_SUB(@base_now, INTERVAL 1 HOUR), 3),
    (@ticket_15, @code_ticket_15, 'Install BI desktop on analyst laptop', 'Se solicita instalar la herramienta de BI en el equipo asignado a un analista nuevo.', 'ASSIGNED', 'MEDIUM', @user_customer_sales, @user_agent_bruno, @category_software, DATE_ADD(@ticket_15_created, INTERVAL 8 HOUR), DATE_ADD(@ticket_15_created, INTERVAL 48 HOUR), NULL, NULL, NULL, NULL, NULL, 0, b'0', b'0', NULL, @ticket_15_created, DATE_SUB(@base_now, INTERVAL 7 HOUR), 2),
    (@ticket_16, @code_ticket_16, 'Shared drive unavailable for finance close', 'La unidad compartida del cierre financiero no responde desde el inicio de la jornada.', 'IN_PROGRESS', 'URGENT', @user_customer_finance, @user_agent_carla, @category_network, DATE_ADD(@ticket_16_created, INTERVAL 2 HOUR), DATE_ADD(@ticket_16_created, INTERVAL 8 HOUR), DATE_ADD(@ticket_16_created, INTERVAL 30 MINUTE), NULL, NULL, NULL, NULL, 0, b'0', b'1', NULL, @ticket_16_created, DATE_SUB(@base_now, INTERVAL 30 MINUTE), 3);

INSERT INTO ticket_comments (id, ticket_id, author_id, content, visibility, created_at, updated_at, version)
VALUES
    ('40000000-0000-0000-0000-000000000001', @ticket_03, @user_agent_ana, 'Se revisaron logs de aplicacion y se detecto un error al exportar a un perfil de usuario sin permisos.', 'INTERNAL', DATE_ADD(@ticket_03_created, INTERVAL 3 HOUR), DATE_ADD(@ticket_03_created, INTERVAL 3 HOUR), 0),
    ('40000000-0000-0000-0000-000000000002', @ticket_03, @user_agent_ana, 'Estamos revisando el evento con el equipo de aplicaciones. Te compartiremos avance en breve.', 'PUBLIC', DATE_ADD(@ticket_03_created, INTERVAL 4 HOUR), DATE_ADD(@ticket_03_created, INTERVAL 4 HOUR), 0),
    ('40000000-0000-0000-0000-000000000003', @ticket_04, @user_agent_bruno, 'Por favor comparte una captura del error, el nombre de la red y el lugar exacto donde ocurre.', 'PUBLIC', DATE_SUB(@base_now, INTERVAL 3 HOUR), DATE_SUB(@base_now, INTERVAL 3 HOUR), 0),
    ('40000000-0000-0000-0000-000000000004', @ticket_05, @user_agent_bruno, 'Se valido el cambio con Microsoft 365 y la membresia ya aparece propagada.', 'INTERNAL', DATE_SUB(@base_now, INTERVAL 7 HOUR), DATE_SUB(@base_now, INTERVAL 7 HOUR), 0),
    ('40000000-0000-0000-0000-000000000005', @ticket_08, @user_agent_carla, 'La espera por restablecimiento de credenciales ya supera el acuerdo de primera respuesta y pone en riesgo la salida a produccion.', 'INTERNAL', DATE_SUB(@base_now, INTERVAL 12 HOUR), DATE_SUB(@base_now, INTERVAL 12 HOUR), 0),
    ('40000000-0000-0000-0000-000000000006', @ticket_12, @user_agent_ana, 'Necesitamos que adjuntes los comprobantes y el numero de referencia para continuar.', 'PUBLIC', DATE_SUB(@base_now, INTERVAL 4 HOUR), DATE_SUB(@base_now, INTERVAL 4 HOUR), 0),
    ('40000000-0000-0000-0000-000000000007', @ticket_13, @user_agent_bruno, 'Por favor confirma la version actual del firmware y el modelo exacto del escaner.', 'PUBLIC', DATE_SUB(@base_now, INTERVAL 5 HOUR), DATE_SUB(@base_now, INTERVAL 5 HOUR), 0),
    ('40000000-0000-0000-0000-000000000008', @ticket_13, @user_customer_finance, 'El equipo tiene firmware 4.2.1 y el modelo es XG Scanner 2400. Adjunto datos validados por bodega.', 'PUBLIC', DATE_SUB(@base_now, INTERVAL 2 HOUR), DATE_SUB(@base_now, INTERVAL 2 HOUR), 0);

INSERT INTO ticket_history (
    id, ticket_id, action, performed_by, previous_value, new_value, metadata_json, created_at
)
VALUES
    ('50000000-0000-0000-0000-000000000001', @ticket_01, 'CREATED', @user_customer_finance, NULL, NULL, CONCAT('{"code":"', @code_ticket_01, '"}'), DATE_ADD(@ticket_01_created, INTERVAL 1 MINUTE)),
    ('50000000-0000-0000-0000-000000000002', @ticket_02, 'CREATED', @user_customer_ops, NULL, NULL, CONCAT('{"code":"', @code_ticket_02, '"}'), DATE_ADD(@ticket_02_created, INTERVAL 1 MINUTE)),
    ('50000000-0000-0000-0000-000000000003', @ticket_02, 'ASSIGNED', @user_manager, NULL, @user_agent_ana, NULL, DATE_ADD(@ticket_02_created, INTERVAL 45 MINUTE)),
    ('50000000-0000-0000-0000-000000000004', @ticket_03, 'CREATED', @user_customer_sales, NULL, NULL, CONCAT('{"code":"', @code_ticket_03, '"}'), DATE_ADD(@ticket_03_created, INTERVAL 1 MINUTE)),
    ('50000000-0000-0000-0000-000000000005', @ticket_03, 'ASSIGNED', @user_manager, NULL, @user_agent_ana, NULL, DATE_ADD(@ticket_03_created, INTERVAL 20 MINUTE)),
    ('50000000-0000-0000-0000-000000000006', @ticket_03, 'STARTED', @user_agent_ana, 'ASSIGNED', 'IN_PROGRESS', NULL, DATE_ADD(@ticket_03_created, INTERVAL 2 HOUR)),
    ('50000000-0000-0000-0000-000000000007', @ticket_03, 'COMMENT_ADDED_INTERNAL', @user_agent_ana, NULL, NULL, '{"commentId":"40000000-0000-0000-0000-000000000001"}', DATE_ADD(@ticket_03_created, INTERVAL 3 HOUR)),
    ('50000000-0000-0000-0000-000000000008', @ticket_03, 'COMMENT_ADDED_PUBLIC', @user_agent_ana, NULL, NULL, '{"commentId":"40000000-0000-0000-0000-000000000002"}', DATE_ADD(@ticket_03_created, INTERVAL 4 HOUR)),
    ('50000000-0000-0000-0000-000000000009', @ticket_04, 'CREATED', @user_customer_finance, NULL, NULL, CONCAT('{"code":"', @code_ticket_04, '"}'), DATE_ADD(@ticket_04_created, INTERVAL 1 MINUTE)),
    ('50000000-0000-0000-0000-000000000010', @ticket_04, 'ASSIGNED', @user_manager, NULL, @user_agent_bruno, NULL, DATE_ADD(@ticket_04_created, INTERVAL 15 MINUTE)),
    ('50000000-0000-0000-0000-000000000011', @ticket_04, 'STARTED', @user_agent_bruno, 'ASSIGNED', 'IN_PROGRESS', NULL, DATE_ADD(@ticket_04_created, INTERVAL 1 HOUR)),
    ('50000000-0000-0000-0000-000000000012', @ticket_04, 'COMMENT_ADDED_PUBLIC', @user_agent_bruno, NULL, NULL, '{"commentId":"40000000-0000-0000-0000-000000000003"}', DATE_SUB(@base_now, INTERVAL 3 HOUR)),
    ('50000000-0000-0000-0000-000000000013', @ticket_04, 'REQUESTED_INFORMATION', @user_agent_bruno, 'IN_PROGRESS', 'WAITING_FOR_CUSTOMER', NULL, DATE_SUB(@base_now, INTERVAL 3 HOUR)),
    ('50000000-0000-0000-0000-000000000014', @ticket_05, 'CREATED', @user_customer_ops, NULL, NULL, CONCAT('{"code":"', @code_ticket_05, '"}'), DATE_ADD(@ticket_05_created, INTERVAL 1 MINUTE)),
    ('50000000-0000-0000-0000-000000000015', @ticket_05, 'ASSIGNED', @user_manager, NULL, @user_agent_bruno, NULL, DATE_ADD(@ticket_05_created, INTERVAL 30 MINUTE)),
    ('50000000-0000-0000-0000-000000000016', @ticket_05, 'STARTED', @user_agent_bruno, 'ASSIGNED', 'IN_PROGRESS', NULL, DATE_ADD(@ticket_05_created, INTERVAL 2 HOUR)),
    ('50000000-0000-0000-0000-000000000017', @ticket_05, 'COMMENT_ADDED_INTERNAL', @user_agent_bruno, NULL, NULL, '{"commentId":"40000000-0000-0000-0000-000000000004"}', DATE_SUB(@base_now, INTERVAL 7 HOUR)),
    ('50000000-0000-0000-0000-000000000018', @ticket_05, 'RESOLVED', @user_agent_bruno, NULL, 'Lista actualizada y validada con el solicitante.', NULL, DATE_SUB(@base_now, INTERVAL 6 HOUR)),
    ('50000000-0000-0000-0000-000000000019', @ticket_06, 'CREATED', @user_customer_hr, NULL, NULL, CONCAT('{"code":"', @code_ticket_06, '"}'), DATE_ADD(@ticket_06_created, INTERVAL 1 MINUTE)),
    ('50000000-0000-0000-0000-000000000020', @ticket_06, 'ASSIGNED', @user_manager, NULL, @user_agent_carla, NULL, DATE_ADD(@ticket_06_created, INTERVAL 2 HOUR)),
    ('50000000-0000-0000-0000-000000000021', @ticket_06, 'STARTED', @user_agent_carla, 'ASSIGNED', 'IN_PROGRESS', NULL, DATE_ADD(@ticket_06_created, INTERVAL 6 HOUR)),
    ('50000000-0000-0000-0000-000000000022', @ticket_06, 'RESOLVED', @user_agent_carla, NULL, 'Equipo entregado, configurado y aceptado por recursos humanos.', NULL, DATE_SUB(@base_now, INTERVAL 120 HOUR)),
    ('50000000-0000-0000-0000-000000000023', @ticket_06, 'CLOSED', @user_customer_hr, NULL, NULL, NULL, DATE_SUB(@base_now, INTERVAL 116 HOUR)),
    ('50000000-0000-0000-0000-000000000024', @ticket_07, 'CREATED', @user_customer_sales, NULL, NULL, CONCAT('{"code":"', @code_ticket_07, '"}'), DATE_ADD(@ticket_07_created, INTERVAL 1 MINUTE)),
    ('50000000-0000-0000-0000-000000000025', @ticket_07, 'CANCELLED', @user_customer_sales, NULL, NULL, '{"reason":"Solicitud duplicada y ya no se requiere atencion."}', DATE_SUB(@base_now, INTERVAL 47 HOUR)),
    ('50000000-0000-0000-0000-000000000026', @ticket_08, 'CREATED', @user_customer_exec, NULL, NULL, CONCAT('{"code":"', @code_ticket_08, '"}'), DATE_ADD(@ticket_08_created, INTERVAL 1 MINUTE)),
    ('50000000-0000-0000-0000-000000000027', @ticket_08, 'ASSIGNED', @user_manager, NULL, @user_agent_carla, NULL, DATE_ADD(@ticket_08_created, INTERVAL 20 MINUTE)),
    ('50000000-0000-0000-0000-000000000028', @ticket_08, 'STARTED', @user_agent_carla, 'ASSIGNED', 'IN_PROGRESS', NULL, DATE_ADD(@ticket_08_created, INTERVAL 6 HOUR)),
    ('50000000-0000-0000-0000-000000000029', @ticket_08, 'COMMENT_ADDED_INTERNAL', @user_agent_carla, NULL, NULL, '{"commentId":"40000000-0000-0000-0000-000000000005"}', DATE_SUB(@base_now, INTERVAL 12 HOUR)),
    ('50000000-0000-0000-0000-000000000030', @ticket_08, 'SLA_BREACHED', @user_manager, 'false', 'true', '{"scope":"first-response,resolution"}', DATE_SUB(@base_now, INTERVAL 12 HOUR)),
    ('50000000-0000-0000-0000-000000000031', @ticket_09, 'CREATED', @user_customer_ops, NULL, NULL, CONCAT('{"code":"', @code_ticket_09, '"}'), DATE_ADD(@ticket_09_created, INTERVAL 1 MINUTE)),
    ('50000000-0000-0000-0000-000000000032', @ticket_09, 'ASSIGNED', @user_admin, NULL, @user_manager, NULL, DATE_ADD(@ticket_09_created, INTERVAL 20 MINUTE)),
    ('50000000-0000-0000-0000-000000000033', @ticket_09, 'STARTED', @user_manager, 'ASSIGNED', 'IN_PROGRESS', NULL, DATE_ADD(@ticket_09_created, INTERVAL 30 MINUTE)),
    ('50000000-0000-0000-0000-000000000034', @ticket_09, 'RESOLVED', @user_manager, NULL, 'Se reinicio el spooler remoto y se liberaron los trabajos pendientes.', NULL, DATE_SUB(@base_now, INTERVAL 10 HOUR)),
    ('50000000-0000-0000-0000-000000000035', @ticket_09, 'SLA_BREACHED', @user_manager, 'false', 'true', '{"scope":"resolution"}', DATE_SUB(@base_now, INTERVAL 10 HOUR)),
    ('50000000-0000-0000-0000-000000000036', @ticket_10, 'CREATED', @user_customer_hr, NULL, NULL, CONCAT('{"code":"', @code_ticket_10, '"}'), DATE_ADD(@ticket_10_created, INTERVAL 1 MINUTE)),
    ('50000000-0000-0000-0000-000000000037', @ticket_10, 'ASSIGNED', @user_manager, NULL, @user_agent_bruno, NULL, DATE_ADD(@ticket_10_created, INTERVAL 30 MINUTE)),
    ('50000000-0000-0000-0000-000000000038', @ticket_10, 'STARTED', @user_agent_bruno, 'ASSIGNED', 'IN_PROGRESS', NULL, DATE_ADD(@ticket_10_created, INTERVAL 2 HOUR)),
    ('50000000-0000-0000-0000-000000000039', @ticket_10, 'RESOLVED', @user_agent_bruno, NULL, 'Se corrigieron los calculos y se regenero el reporte.', NULL, DATE_SUB(@base_now, INTERVAL 30 HOUR)),
    ('50000000-0000-0000-0000-000000000040', @ticket_10, 'REOPENED', @user_customer_hr, 'RESOLVED', 'IN_PROGRESS', '{"reason":"Faltan asientos del ultimo lote y se requiere un nuevo procesamiento."}', DATE_SUB(@base_now, INTERVAL 18 HOUR)),
    ('50000000-0000-0000-0000-000000000041', @ticket_11, 'CREATED', @user_customer_exec, NULL, NULL, CONCAT('{"code":"', @code_ticket_11, '"}'), DATE_ADD(@ticket_11_created, INTERVAL 1 MINUTE)),
    ('50000000-0000-0000-0000-000000000042', @ticket_11, 'ASSIGNED', @user_manager, NULL, @user_agent_carla, NULL, DATE_ADD(@ticket_11_created, INTERVAL 10 MINUTE)),
    ('50000000-0000-0000-0000-000000000043', @ticket_12, 'CREATED', @user_customer_ops, NULL, NULL, CONCAT('{"code":"', @code_ticket_12, '"}'), DATE_ADD(@ticket_12_created, INTERVAL 1 MINUTE)),
    ('50000000-0000-0000-0000-000000000044', @ticket_12, 'ASSIGNED', @user_manager, NULL, @user_agent_ana, NULL, DATE_ADD(@ticket_12_created, INTERVAL 20 MINUTE)),
    ('50000000-0000-0000-0000-000000000045', @ticket_12, 'STARTED', @user_agent_ana, 'ASSIGNED', 'IN_PROGRESS', NULL, DATE_ADD(@ticket_12_created, INTERVAL 1 HOUR)),
    ('50000000-0000-0000-0000-000000000046', @ticket_12, 'COMMENT_ADDED_PUBLIC', @user_agent_ana, NULL, NULL, '{"commentId":"40000000-0000-0000-0000-000000000006"}', DATE_SUB(@base_now, INTERVAL 4 HOUR)),
    ('50000000-0000-0000-0000-000000000047', @ticket_12, 'REQUESTED_INFORMATION', @user_agent_ana, 'IN_PROGRESS', 'WAITING_FOR_CUSTOMER', NULL, DATE_SUB(@base_now, INTERVAL 4 HOUR)),
    ('50000000-0000-0000-0000-000000000048', @ticket_13, 'CREATED', @user_customer_finance, NULL, NULL, CONCAT('{"code":"', @code_ticket_13, '"}'), DATE_ADD(@ticket_13_created, INTERVAL 1 MINUTE)),
    ('50000000-0000-0000-0000-000000000049', @ticket_13, 'ASSIGNED', @user_manager, NULL, @user_agent_bruno, NULL, DATE_ADD(@ticket_13_created, INTERVAL 30 MINUTE)),
    ('50000000-0000-0000-0000-000000000050', @ticket_13, 'STARTED', @user_agent_bruno, 'ASSIGNED', 'IN_PROGRESS', NULL, DATE_ADD(@ticket_13_created, INTERVAL 2 HOUR)),
    ('50000000-0000-0000-0000-000000000051', @ticket_13, 'COMMENT_ADDED_PUBLIC', @user_agent_bruno, NULL, NULL, '{"commentId":"40000000-0000-0000-0000-000000000007"}', DATE_SUB(@base_now, INTERVAL 5 HOUR)),
    ('50000000-0000-0000-0000-000000000052', @ticket_13, 'REQUESTED_INFORMATION', @user_agent_bruno, 'IN_PROGRESS', 'WAITING_FOR_CUSTOMER', NULL, DATE_SUB(@base_now, INTERVAL 5 HOUR)),
    ('50000000-0000-0000-0000-000000000053', @ticket_13, 'COMMENT_ADDED_PUBLIC', @user_customer_finance, NULL, NULL, '{"commentId":"40000000-0000-0000-0000-000000000008"}', DATE_SUB(@base_now, INTERVAL 2 HOUR)),
    ('50000000-0000-0000-0000-000000000054', @ticket_14, 'CREATED', @user_customer_hr, NULL, NULL, CONCAT('{"code":"', @code_ticket_14, '"}'), DATE_ADD(@ticket_14_created, INTERVAL 1 MINUTE)),
    ('50000000-0000-0000-0000-000000000055', @ticket_14, 'UPDATED', @user_customer_hr, 'Request access removed', 'Request access removed by mistake', NULL, DATE_SUB(@base_now, INTERVAL 90 MINUTE)),
    ('50000000-0000-0000-0000-000000000056', @ticket_14, 'PRIORITY_CHANGED', @user_customer_hr, 'LOW', 'MEDIUM', NULL, DATE_SUB(@base_now, INTERVAL 80 MINUTE)),
    ('50000000-0000-0000-0000-000000000057', @ticket_14, 'CATEGORY_CHANGED', @user_customer_hr, @category_service, @category_access, NULL, DATE_SUB(@base_now, INTERVAL 70 MINUTE)),
    ('50000000-0000-0000-0000-000000000058', @ticket_15, 'CREATED', @user_customer_sales, NULL, NULL, CONCAT('{"code":"', @code_ticket_15, '"}'), DATE_ADD(@ticket_15_created, INTERVAL 1 MINUTE)),
    ('50000000-0000-0000-0000-000000000059', @ticket_15, 'ASSIGNED', @user_manager, NULL, @user_agent_ana, NULL, DATE_ADD(@ticket_15_created, INTERVAL 20 MINUTE)),
    ('50000000-0000-0000-0000-000000000060', @ticket_15, 'REASSIGNED', @user_manager, @user_agent_ana, @user_agent_bruno, NULL, DATE_SUB(@base_now, INTERVAL 7 HOUR)),
    ('50000000-0000-0000-0000-000000000061', @ticket_16, 'CREATED', @user_customer_finance, NULL, NULL, CONCAT('{"code":"', @code_ticket_16, '"}'), DATE_ADD(@ticket_16_created, INTERVAL 1 MINUTE)),
    ('50000000-0000-0000-0000-000000000062', @ticket_16, 'ASSIGNED', @user_manager, NULL, @user_agent_carla, NULL, DATE_ADD(@ticket_16_created, INTERVAL 10 MINUTE)),
    ('50000000-0000-0000-0000-000000000063', @ticket_16, 'STARTED', @user_agent_carla, 'ASSIGNED', 'IN_PROGRESS', NULL, DATE_ADD(@ticket_16_created, INTERVAL 30 MINUTE)),
    ('50000000-0000-0000-0000-000000000064', @ticket_16, 'SLA_BREACHED', @user_agent_carla, 'false', 'true', '{"scope":"resolution"}', DATE_SUB(@base_now, INTERVAL 30 MINUTE));

INSERT INTO notifications (
    id, recipient_id, type, title, message, related_ticket_id, is_read, read_at, created_at, updated_at, version
)
VALUES
    ('60000000-0000-0000-0000-000000000001', @user_admin, 'TICKET_CREATED', 'Nuevo ticket creado', CONCAT('Se creo el ticket ', @code_ticket_01, '.'), @ticket_01, b'0', NULL, DATE_ADD(@ticket_01_created, INTERVAL 2 MINUTE), DATE_ADD(@ticket_01_created, INTERVAL 2 MINUTE), 0),
    ('60000000-0000-0000-0000-000000000002', @user_manager, 'TICKET_CREATED', 'Nuevo ticket creado', CONCAT('Se creo el ticket ', @code_ticket_01, '.'), @ticket_01, b'1', DATE_ADD(@ticket_01_created, INTERVAL 10 MINUTE), DATE_ADD(@ticket_01_created, INTERVAL 2 MINUTE), DATE_ADD(@ticket_01_created, INTERVAL 10 MINUTE), 0),
    ('60000000-0000-0000-0000-000000000003', @user_agent_ana, 'TICKET_ASSIGNED', 'Ticket asignado', CONCAT('Se te asigno el ticket ', @code_ticket_02, '.'), @ticket_02, b'1', DATE_ADD(@ticket_02_created, INTERVAL 90 MINUTE), DATE_ADD(@ticket_02_created, INTERVAL 45 MINUTE), DATE_ADD(@ticket_02_created, INTERVAL 90 MINUTE), 0),
    ('60000000-0000-0000-0000-000000000004', @user_customer_sales, 'PUBLIC_COMMENT_ADDED', 'Nuevo comentario', CONCAT('Hay un nuevo comentario en el ticket ', @code_ticket_03, '.'), @ticket_03, b'1', DATE_ADD(@ticket_03_created, INTERVAL 5 HOUR), DATE_ADD(@ticket_03_created, INTERVAL 4 HOUR), DATE_ADD(@ticket_03_created, INTERVAL 5 HOUR), 0),
    ('60000000-0000-0000-0000-000000000005', @user_customer_finance, 'INFORMATION_REQUESTED', 'Se requiere informacion', CONCAT('Hay una solicitud de informacion en el ticket ', @code_ticket_04, '.'), @ticket_04, b'0', NULL, DATE_SUB(@base_now, INTERVAL 3 HOUR), DATE_SUB(@base_now, INTERVAL 3 HOUR), 0),
    ('60000000-0000-0000-0000-000000000006', @user_customer_ops, 'TICKET_RESOLVED', 'Ticket resuelto', CONCAT('El ticket ', @code_ticket_05, ' fue resuelto.'), @ticket_05, b'1', DATE_SUB(@base_now, INTERVAL 5 HOUR), DATE_SUB(@base_now, INTERVAL 6 HOUR), DATE_SUB(@base_now, INTERVAL 5 HOUR), 0),
    ('60000000-0000-0000-0000-000000000007', @user_customer_hr, 'TICKET_CLOSED', 'Ticket cerrado', CONCAT('El ticket ', @code_ticket_06, ' fue cerrado.'), @ticket_06, b'1', DATE_SUB(@base_now, INTERVAL 115 HOUR), DATE_SUB(@base_now, INTERVAL 116 HOUR), DATE_SUB(@base_now, INTERVAL 115 HOUR), 0),
    ('60000000-0000-0000-0000-000000000008', @user_manager, 'SLA_BREACHED', 'SLA incumplido', CONCAT('El ticket ', @code_ticket_08, ' presenta un incumplimiento de SLA.'), @ticket_08, b'0', NULL, DATE_SUB(@base_now, INTERVAL 12 HOUR), DATE_SUB(@base_now, INTERVAL 12 HOUR), 0),
    ('60000000-0000-0000-0000-000000000009', @user_customer_ops, 'TICKET_RESOLVED', 'Ticket resuelto', CONCAT('El ticket ', @code_ticket_09, ' fue resuelto.'), @ticket_09, b'0', NULL, DATE_SUB(@base_now, INTERVAL 10 HOUR), DATE_SUB(@base_now, INTERVAL 10 HOUR), 0),
    ('60000000-0000-0000-0000-000000000010', @user_agent_bruno, 'TICKET_REOPENED', 'Ticket reabierto', CONCAT('El ticket ', @code_ticket_10, ' fue reabierto.'), @ticket_10, b'0', NULL, DATE_SUB(@base_now, INTERVAL 18 HOUR), DATE_SUB(@base_now, INTERVAL 18 HOUR), 0),
    ('60000000-0000-0000-0000-000000000011', @user_agent_carla, 'TICKET_ASSIGNED', 'Ticket asignado', CONCAT('Se te asigno el ticket ', @code_ticket_11, '.'), @ticket_11, b'0', NULL, DATE_ADD(@ticket_11_created, INTERVAL 10 MINUTE), DATE_ADD(@ticket_11_created, INTERVAL 10 MINUTE), 0),
    ('60000000-0000-0000-0000-000000000012', @user_manager, 'SLA_DUE_SOON', 'SLA por vencer', CONCAT('El ticket ', @code_ticket_11, ' esta cerca de vencer su SLA.'), @ticket_11, b'0', NULL, DATE_SUB(@base_now, INTERVAL 20 MINUTE), DATE_SUB(@base_now, INTERVAL 20 MINUTE), 0),
    ('60000000-0000-0000-0000-000000000013', @user_customer_ops, 'INFORMATION_REQUESTED', 'Se requiere informacion', CONCAT('Hay una solicitud de informacion en el ticket ', @code_ticket_12, '.'), @ticket_12, b'0', NULL, DATE_SUB(@base_now, INTERVAL 4 HOUR), DATE_SUB(@base_now, INTERVAL 4 HOUR), 0),
    ('60000000-0000-0000-0000-000000000014', @user_agent_bruno, 'PUBLIC_COMMENT_ADDED', 'Respuesta del cliente', CONCAT('El cliente respondio en el ticket ', @code_ticket_13, '.'), @ticket_13, b'0', NULL, DATE_SUB(@base_now, INTERVAL 2 HOUR), DATE_SUB(@base_now, INTERVAL 2 HOUR), 0),
    ('60000000-0000-0000-0000-000000000015', @user_admin, 'TICKET_CREATED', 'Nuevo ticket creado', CONCAT('Se creo el ticket ', @code_ticket_14, '.'), @ticket_14, b'0', NULL, DATE_ADD(@ticket_14_created, INTERVAL 2 MINUTE), DATE_ADD(@ticket_14_created, INTERVAL 2 MINUTE), 0),
    ('60000000-0000-0000-0000-000000000016', @user_agent_bruno, 'TICKET_ASSIGNED', 'Ticket asignado', CONCAT('Se te asigno el ticket ', @code_ticket_15, '.'), @ticket_15, b'0', NULL, DATE_SUB(@base_now, INTERVAL 7 HOUR), DATE_SUB(@base_now, INTERVAL 7 HOUR), 0),
    ('60000000-0000-0000-0000-000000000017', @user_agent_carla, 'SLA_BREACHED', 'SLA incumplido', CONCAT('El ticket ', @code_ticket_16, ' supero su tiempo objetivo de resolucion.'), @ticket_16, b'0', NULL, DATE_SUB(@base_now, INTERVAL 30 MINUTE), DATE_SUB(@base_now, INTERVAL 30 MINUTE), 0),
    ('60000000-0000-0000-0000-000000000018', @user_manager, 'SLA_BREACHED', 'SLA incumplido', CONCAT('El ticket ', @code_ticket_16, ' supero su tiempo objetivo de resolucion.'), @ticket_16, b'0', NULL, DATE_SUB(@base_now, INTERVAL 30 MINUTE), DATE_SUB(@base_now, INTERVAL 30 MINUTE), 0),
    ('60000000-0000-0000-0000-000000000019', @user_manager, 'TICKET_ASSIGNED', 'Ticket asignado', CONCAT('Se te asigno el ticket ', @code_ticket_09, '.'), @ticket_09, b'1', DATE_ADD(@ticket_09_created, INTERVAL 30 MINUTE), DATE_ADD(@ticket_09_created, INTERVAL 20 MINUTE), DATE_ADD(@ticket_09_created, INTERVAL 30 MINUTE), 0),
    ('60000000-0000-0000-0000-000000000020', @user_manager, 'TICKET_CREATED', 'Nuevo ticket creado', CONCAT('Se creo el ticket ', @code_ticket_11, '.'), @ticket_11, b'0', NULL, DATE_ADD(@ticket_11_created, INTERVAL 1 MINUTE), DATE_ADD(@ticket_11_created, INTERVAL 1 MINUTE), 0),
    ('60000000-0000-0000-0000-000000000021', @user_admin, 'TICKET_CREATED', 'Nuevo ticket creado', CONCAT('Se creo el ticket ', @code_ticket_11, '.'), @ticket_11, b'0', NULL, DATE_ADD(@ticket_11_created, INTERVAL 1 MINUTE), DATE_ADD(@ticket_11_created, INTERVAL 1 MINUTE), 0),
    ('60000000-0000-0000-0000-000000000022', @user_manager, 'TICKET_CREATED', 'Nuevo ticket creado', CONCAT('Se creo el ticket ', @code_ticket_14, '.'), @ticket_14, b'1', DATE_SUB(@base_now, INTERVAL 80 MINUTE), DATE_ADD(@ticket_14_created, INTERVAL 2 MINUTE), DATE_SUB(@base_now, INTERVAL 80 MINUTE), 0),
    ('60000000-0000-0000-0000-000000000023', @user_admin, 'TICKET_CREATED', 'Nuevo ticket creado', CONCAT('Se creo el ticket ', @code_ticket_16, '.'), @ticket_16, b'0', NULL, DATE_ADD(@ticket_16_created, INTERVAL 1 MINUTE), DATE_ADD(@ticket_16_created, INTERVAL 1 MINUTE), 0),
    ('60000000-0000-0000-0000-000000000024', @user_customer_ops, 'PUBLIC_COMMENT_ADDED', 'Nuevo comentario', CONCAT('Hay un nuevo comentario en el ticket ', @code_ticket_12, '.'), @ticket_12, b'1', DATE_SUB(@base_now, INTERVAL 3 HOUR), DATE_SUB(@base_now, INTERVAL 4 HOUR), DATE_SUB(@base_now, INTERVAL 3 HOUR), 0),
    ('60000000-0000-0000-0000-000000000025', @user_admin, 'SLA_BREACHED', 'SLA incumplido', CONCAT('El ticket ', @code_ticket_08, ' presenta un incumplimiento de SLA.'), @ticket_08, b'0', NULL, DATE_SUB(@base_now, INTERVAL 12 HOUR), DATE_SUB(@base_now, INTERVAL 12 HOUR), 0);

INSERT INTO ticket_sequences (sequence_year, current_value)
VALUES (@current_year, 900016)
ON DUPLICATE KEY UPDATE current_value = GREATEST(current_value, VALUES(current_value));

COMMIT;
