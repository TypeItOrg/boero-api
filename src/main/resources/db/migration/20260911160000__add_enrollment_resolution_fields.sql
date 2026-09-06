ALTER TABLE enrollment_applications ADD COLUMN IF NOT EXISTS rejection_reason text;
ALTER TABLE enrollment_applications ADD COLUMN IF NOT EXISTS resolved_at timestamp(6);

ALTER TABLE enrollment_applications DROP CONSTRAINT IF EXISTS enrollment_applications_status_check;
ALTER TABLE enrollment_applications ADD CONSTRAINT enrollment_applications_status_check
    CHECK (status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED', 'CANCELLED'));

ALTER TABLE enrollment_applications DROP CONSTRAINT IF EXISTS enrollment_applications_resolution_consistency_check;
ALTER TABLE enrollment_applications ADD CONSTRAINT enrollment_applications_resolution_consistency_check
    CHECK ((status = 'REJECTED') = (rejection_reason IS NOT NULL));

CREATE INDEX IF NOT EXISTS enrollment_applications_institution_status_idx
    ON enrollment_applications (institution_id, status)
    WHERE deleted_at IS NULL;

INSERT INTO permissions (created_at, updated_at, permission_id, scope, code, description)
VALUES
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:enrollment-application:read')::uuid, 'INSTITUTION', 'institution:enrollment-application:read', 'Ver solicitudes de inscripción'),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:enrollment-application:approve')::uuid, 'INSTITUTION', 'institution:enrollment-application:approve', 'Aprobar solicitudes de inscripción'),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:enrollment-application:reject')::uuid, 'INSTITUTION', 'institution:enrollment-application:reject', 'Rechazar solicitudes de inscripción')
ON CONFLICT (code) DO UPDATE
SET description = EXCLUDED.description,
    scope = EXCLUDED.scope,
    updated_at = CURRENT_TIMESTAMP;
