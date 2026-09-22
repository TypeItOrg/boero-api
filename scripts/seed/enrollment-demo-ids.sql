-- Local fixture infrastructure only; never install through Flyway or in production.
-- Concatenate before enrollment-demo.sql or enrollment-demo-repair-ids.sql.
\set ON_ERROR_STOP on
BEGIN;
SET LOCAL lock_timeout = '10s';
SELECT pg_advisory_xact_lock(hashtext('boero:enrollment-demo:v1'));
CREATE SCHEMA IF NOT EXISTS boero_demo;
CREATE TABLE IF NOT EXISTS boero_demo.identifiers (
    namespace text NOT NULL CHECK (namespace IN ('boero:enrollment-demo:v1:', 'boero:enrollment:official-2025:')),
    seed_key text NOT NULL,
    legacy_id uuid NOT NULL UNIQUE,
    id uuid NOT NULL UNIQUE DEFAULT uuidv7() CHECK (uuid_extract_version(id) = 7),
    PRIMARY KEY (namespace, seed_key)
);

-- Nested fixture keys contain other IDs. Normalize them to their original values
-- so repairing a parent never changes the logical identity of its children.
CREATE FUNCTION pg_temp.seed_key(key text) RETURNS text LANGUAGE plpgsql AS $$
DECLARE
    embedded text[];
    original uuid;
BEGIN
    FOR embedded IN SELECT regexp_matches(key, '[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}', 'g') LOOP
        SELECT legacy_id INTO original FROM boero_demo.identifiers WHERE id = embedded[1]::uuid;
        IF original IS NOT NULL THEN
            key := replace(key, embedded[1], original::text);
        END IF;
    END LOOP;
    RETURN key;
END $$;

CREATE FUNCTION pg_temp.seed_id(seed_namespace text, key text) RETURNS uuid LANGUAGE plpgsql AS $$
DECLARE
    canonical_key text := pg_temp.seed_key(key);
    result uuid;
BEGIN
    INSERT INTO boero_demo.identifiers (namespace, seed_key, legacy_id)
    VALUES (seed_namespace, canonical_key, md5(seed_namespace || canonical_key)::uuid)
    ON CONFLICT (namespace, seed_key) DO NOTHING;
    SELECT id INTO STRICT result FROM boero_demo.identifiers
    WHERE namespace = seed_namespace AND seed_key = canonical_key;
    RETURN result;
END $$;

CREATE FUNCTION pg_temp.demo_id(key text) RETURNS uuid LANGUAGE sql AS
$$ SELECT pg_temp.seed_id('boero:enrollment-demo:v1:', key) $$;
CREATE FUNCTION pg_temp.academic_id(key text) RETURNS uuid LANGUAGE sql AS
$$ SELECT pg_temp.seed_id('boero:enrollment:official-2025:', key) $$;
