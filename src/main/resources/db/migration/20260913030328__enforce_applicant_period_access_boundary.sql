DELETE FROM role_permissions role_permission
USING roles role, permissions permission
WHERE role_permission.role_id = role.role_id
  AND role_permission.permission_id = permission.permission_id
  AND role.scope = 'INSTITUTION'
  AND role.is_system = true
  AND role.code IN ('APPLICANT', 'STUDENT')
  AND permission.code = 'institution:enrollment-period:read';
