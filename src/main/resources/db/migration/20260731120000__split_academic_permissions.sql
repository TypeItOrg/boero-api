INSERT INTO permissions (created_at, updated_at, permission_id, scope, code, description)
VALUES
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:academic-year:read')::uuid, 'INSTITUTION', 'institution:academic-year:read', 'Ver ciclos lectivos'),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:academic-year:create')::uuid, 'INSTITUTION', 'institution:academic-year:create', 'Crear ciclos lectivos'),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:academic-year:update')::uuid, 'INSTITUTION', 'institution:academic-year:update', 'Editar ciclos lectivos'),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:academic-year:update-status')::uuid, 'INSTITUTION', 'institution:academic-year:update-status', 'Cambiar estado de ciclos lectivos'),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:training-path:read')::uuid, 'INSTITUTION', 'institution:training-path:read', 'Ver trayectos formativos'),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:training-path:create')::uuid, 'INSTITUTION', 'institution:training-path:create', 'Crear trayectos formativos'),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:training-path:update')::uuid, 'INSTITUTION', 'institution:training-path:update', 'Editar trayectos formativos'),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:training-path:update-status')::uuid, 'INSTITUTION', 'institution:training-path:update-status', 'Cambiar estado de trayectos formativos'),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:study-plan:read')::uuid, 'INSTITUTION', 'institution:study-plan:read', 'Ver planes de estudio'),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:study-plan:create')::uuid, 'INSTITUTION', 'institution:study-plan:create', 'Crear planes de estudio'),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:study-plan:update')::uuid, 'INSTITUTION', 'institution:study-plan:update', 'Editar planes de estudio'),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:study-plan:update-status')::uuid, 'INSTITUTION', 'institution:study-plan:update-status', 'Cambiar estado de planes de estudio'),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:study-plan:curriculum-update')::uuid, 'INSTITUTION', 'institution:study-plan:curriculum-update', 'Editar estructura curricular'),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:academic-space:read')::uuid, 'INSTITUTION', 'institution:academic-space:read', 'Ver espacios académicos'),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:academic-space:create')::uuid, 'INSTITUTION', 'institution:academic-space:create', 'Crear espacios académicos'),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:academic-space:update')::uuid, 'INSTITUTION', 'institution:academic-space:update', 'Editar espacios académicos'),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:academic-space:update-status')::uuid, 'INSTITUTION', 'institution:academic-space:update-status', 'Cambiar estado de espacios académicos'),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:instrument:read')::uuid, 'INSTITUTION', 'institution:instrument:read', 'Ver instrumentos'),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:instrument:create')::uuid, 'INSTITUTION', 'institution:instrument:create', 'Crear instrumentos'),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:instrument:update')::uuid, 'INSTITUTION', 'institution:instrument:update', 'Editar instrumentos'),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:instrument:update-status')::uuid, 'INSTITUTION', 'institution:instrument:update-status', 'Cambiar estado de instrumentos')
ON CONFLICT (code) DO UPDATE
SET description = EXCLUDED.description,
    scope = EXCLUDED.scope,
    updated_at = CURRENT_TIMESTAMP;

WITH permission_mappings(old_code, new_code) AS (
    VALUES
        ('institution:academic-year:manage', 'institution:academic-year:read'),
        ('institution:academic-year:manage', 'institution:academic-year:create'),
        ('institution:academic-year:manage', 'institution:academic-year:update'),
        ('institution:academic-year:manage', 'institution:academic-year:update-status'),
        ('institution:training-path:manage', 'institution:training-path:read'),
        ('institution:training-path:manage', 'institution:training-path:create'),
        ('institution:training-path:manage', 'institution:training-path:update'),
        ('institution:training-path:manage', 'institution:training-path:update-status'),
        ('institution:study-plan:manage', 'institution:study-plan:read'),
        ('institution:study-plan:manage', 'institution:study-plan:create'),
        ('institution:study-plan:manage', 'institution:study-plan:update'),
        ('institution:study-plan:manage', 'institution:study-plan:update-status'),
        ('institution:study-plan:manage', 'institution:study-plan:curriculum-update'),
        ('institution:academic-catalog:read', 'institution:academic-space:read'),
        ('institution:academic-catalog:read', 'institution:instrument:read'),
        ('institution:academic-catalog:manage', 'institution:academic-space:read'),
        ('institution:academic-catalog:manage', 'institution:academic-space:create'),
        ('institution:academic-catalog:manage', 'institution:academic-space:update'),
        ('institution:academic-catalog:manage', 'institution:academic-space:update-status'),
        ('institution:academic-catalog:manage', 'institution:instrument:read'),
        ('institution:academic-catalog:manage', 'institution:instrument:create'),
        ('institution:academic-catalog:manage', 'institution:instrument:update'),
        ('institution:academic-catalog:manage', 'institution:instrument:update-status')
)
INSERT INTO role_permissions (permission_id, role_id)
SELECT new_permission.permission_id, role_permission.role_id
FROM role_permissions role_permission
JOIN permissions old_permission
    ON old_permission.permission_id = role_permission.permission_id
JOIN permission_mappings
    ON permission_mappings.old_code = old_permission.code
JOIN permissions new_permission
    ON new_permission.code = permission_mappings.new_code
ON CONFLICT (permission_id, role_id) DO NOTHING;

DELETE FROM role_permissions
WHERE permission_id IN (
    SELECT permission_id
    FROM permissions
    WHERE code IN (
        'institution:academic-year:manage',
        'institution:training-path:manage',
        'institution:study-plan:manage',
        'institution:academic-catalog:read',
        'institution:academic-catalog:manage'
    )
);

DELETE FROM permissions
WHERE code IN (
    'institution:academic-year:manage',
    'institution:training-path:manage',
    'institution:study-plan:manage',
    'institution:academic-catalog:read',
    'institution:academic-catalog:manage'
);
