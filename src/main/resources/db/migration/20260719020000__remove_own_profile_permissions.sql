DELETE FROM role_permissions
WHERE permission_id IN (
    SELECT permission_id
    FROM permissions
    WHERE code IN ('institution:person:read-own', 'institution:person:update-own')
);

DELETE FROM permissions
WHERE code IN ('institution:person:read-own', 'institution:person:update-own');
