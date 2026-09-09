INSERT INTO permissions (created_at, updated_at, permission_id, scope, code, description)
VALUES (
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    md5('institution:academic-offer:read')::uuid,
    'INSTITUTION',
    'institution:academic-offer:read',
    'Consultar oferta académica disponible'
)
ON CONFLICT (code) DO UPDATE
SET description = EXCLUDED.description,
    scope = EXCLUDED.scope,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO role_permissions (permission_id, role_id)
SELECT permission.permission_id, role.role_id
FROM permissions permission
JOIN roles role
    ON role.scope = 'INSTITUTION'
   AND role.is_system = true
   AND role.code IN ('APPLICANT', 'INSTITUTIONAL_AUTHORITY')
WHERE permission.code = 'institution:academic-offer:read'
ON CONFLICT (permission_id, role_id) DO NOTHING;
