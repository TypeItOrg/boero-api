WITH dependencies(dependent_code, required_code) AS (
    VALUES
        ('institution:person:create', 'institution:person:read-any'),
        ('institution:person:update-any', 'institution:person:read-any'),
        ('institution:person:delete', 'institution:person:read-any'),
        ('institution:users:update-status', 'institution:person:read-any'),
        ('institution:roles:assign', 'institution:person:read-any'),
        ('institution:roles:assign', 'institution:roles:read'),
        ('institution:roles:revoke', 'institution:person:read-any'),
        ('institution:roles:revoke', 'institution:roles:read'),
        ('institution:roles:create', 'institution:roles:read'),
        ('institution:roles:update', 'institution:roles:read'),
        ('institution:roles:delete', 'institution:roles:read')
)
INSERT INTO role_permissions (permission_id, role_id)
SELECT required_permission.permission_id, granted_role_permission.role_id
FROM role_permissions granted_role_permission
JOIN permissions granted_permission
    ON granted_permission.permission_id = granted_role_permission.permission_id
JOIN dependencies
    ON dependencies.dependent_code = granted_permission.code
JOIN permissions required_permission
    ON required_permission.code = dependencies.required_code
ON CONFLICT (permission_id, role_id) DO NOTHING;
