CREATE TABLE enrollment_document_audit (
    id uuid PRIMARY KEY,
    occurred_at timestamptz NOT NULL,
    actor_id uuid,
    account_type varchar(20) NOT NULL CHECK (account_type IN ('INSTITUTION', 'PLATFORM', 'SYSTEM', 'ANONYMOUS')),
    institution_id uuid,
    application_id uuid,
    attachment_id uuid,
    action varchar(40) NOT NULL CHECK (action IN ('METADATA_READ', 'CONTENT_ACCESS', 'UPLOAD', 'REPLACE', 'DELETE', 'PHYSICAL_DELETE')),
    result varchar(20) NOT NULL CHECK (result IN ('ALLOWED', 'DENIED', 'SUCCESS', 'RETRY')),
    request_id varchar(36)
);
CREATE INDEX enrollment_document_audit_application_idx
    ON enrollment_document_audit (application_id, occurred_at DESC);
CREATE INDEX enrollment_document_audit_institution_idx
    ON enrollment_document_audit (institution_id, occurred_at DESC);

-- No business foreign keys: audit and cleanup must survive deletion of their roots.
CREATE TABLE enrollment_storage_jobs (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    state varchar(12) NOT NULL CHECK (state IN ('PENDING', 'ACTIVE', 'DONE')),
    provider varchar(10) NOT NULL CHECK (provider IN ('local', 's3')),
    location varchar(1024) NOT NULL,
    prefix varchar(500) NOT NULL,
    storage_path varchar(500) NOT NULL,
    institution_id uuid NOT NULL,
    application_id uuid NOT NULL,
    attachment_id uuid,
    actor_id uuid,
    account_type varchar(20) NOT NULL,
    request_id varchar(36),
    attempts integer NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    next_attempt_at timestamptz,
    last_error varchar(100),
    CHECK ((state = 'PENDING') = (next_attempt_at IS NOT NULL)),
    CHECK (state <> 'ACTIVE' OR attachment_id IS NOT NULL)
);
CREATE INDEX enrollment_storage_jobs_pending_idx ON enrollment_storage_jobs (next_attempt_at)
    WHERE state = 'PENDING';
CREATE UNIQUE INDEX enrollment_storage_jobs_attachment_idx ON enrollment_storage_jobs (attachment_id)
    WHERE attachment_id IS NOT NULL;
CREATE INDEX enrollment_storage_jobs_path_idx ON enrollment_storage_jobs (storage_path);
