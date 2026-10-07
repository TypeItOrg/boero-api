-- Explicit, development-only preparation before applying configurable document migrations.
-- Discards the old fixed-type metadata; never touches the new versioned model.
BEGIN;
DO $$
DECLARE discarded_count bigint;
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema() AND table_name = 'enrollment_attachments'
          AND column_name = 'attachment_type'
    ) THEN
        RAISE NOTICE 'No legacy enrollment documents to discard';
        RETURN;
    END IF;

    LOCK TABLE enrollment_attachments IN ACCESS EXCLUSIVE MODE;
    -- Keep known object destinations durable; the worker can remove them after migration.
    IF to_regclass('enrollment_storage_jobs') IS NOT NULL THEN
        UPDATE enrollment_storage_jobs job
        SET state = 'PENDING', next_attempt_at = now(), updated_at = now()
        WHERE job.state = 'ACTIVE' AND EXISTS (
            SELECT 1 FROM enrollment_attachments attachment
            WHERE attachment.enrollment_attachment_id = job.attachment_id
        );
    END IF;
    DELETE FROM enrollment_attachments;
    GET DIAGNOSTICS discarded_count = ROW_COUNT;
    RAISE NOTICE 'Discarded % legacy attachment records', discarded_count;
END $$;
COMMIT;
