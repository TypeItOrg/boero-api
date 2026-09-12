ALTER TABLE passkey_credentials
  ALTER COLUMN created_at TYPE TIMESTAMP(6) WITH TIME ZONE USING created_at AT TIME ZONE 'UTC',
  ALTER COLUMN updated_at TYPE TIMESTAMP(6) WITH TIME ZONE USING updated_at AT TIME ZONE 'UTC',
  ALTER COLUMN last_used_at TYPE TIMESTAMP(6) WITH TIME ZONE USING last_used_at AT TIME ZONE 'America/Argentina/Buenos_Aires',
  ALTER COLUMN revoked_at TYPE TIMESTAMP(6) WITH TIME ZONE USING revoked_at AT TIME ZONE 'America/Argentina/Buenos_Aires';
