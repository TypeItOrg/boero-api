-- The previous HTTP client wrote UTC ISO instants into these zone-less columns.
-- Preserve those stored clock values as UTC; do not reinterpret them as local time.
ALTER TABLE enrollment_periods
    ALTER COLUMN start_date TYPE timestamptz USING start_date AT TIME ZONE 'UTC',
    ALTER COLUMN end_date TYPE timestamptz USING end_date AT TIME ZONE 'UTC';
ALTER TABLE enrollment_applications
    ALTER COLUMN resolved_at TYPE timestamptz USING resolved_at AT TIME ZONE 'UTC';

-- Existing duplicates require review; never discard attachments in a migration.
CREATE UNIQUE INDEX enrollment_attachments_active_type_unique
    ON enrollment_attachments (enrollment_application_id, attachment_type)
    WHERE deleted_at IS NULL;
