DO $$
DECLARE
    event_column record;
BEGIN
    FOR event_column IN
        SELECT
            table_schema,
            table_name,
            column_name,
            CASE
                WHEN column_name IN ('expires_at', 'used_at', 'ended_at', 'deleted_at')
                    THEN 'America/Argentina/Buenos_Aires'
                ELSE 'UTC'
            END AS source_zone
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND data_type = 'timestamp without time zone'
          AND column_name IN (
              'created_at', 'updated_at', 'deleted_at',
              'started_at', 'ended_at', 'expires_at', 'used_at'
          )
        ORDER BY table_name, ordinal_position
    LOOP
        EXECUTE format(
            'ALTER TABLE %I.%I ALTER COLUMN %I TYPE timestamp(6) with time zone USING %I AT TIME ZONE %L',
            event_column.table_schema,
            event_column.table_name,
            event_column.column_name,
            event_column.column_name,
            event_column.source_zone
        );
    END LOOP;
END $$;
