ALTER TABLE institutions
    ADD COLUMN public_subdomain VARCHAR(63),
    ADD COLUMN logo_key VARCHAR(255),
    ADD COLUMN logo_content_type VARCHAR(32),
    ADD COLUMN logo_version VARCHAR(36),
    ADD COLUMN logo_size BIGINT,
    ADD CONSTRAINT institutions_public_subdomain_unique UNIQUE (public_subdomain),
    ADD CONSTRAINT institutions_public_subdomain_dns_check CHECK (
        public_subdomain IS NULL OR public_subdomain ~ '^[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?$'
    ),
    ADD CONSTRAINT institutions_public_subdomain_reserved_check CHECK (
        public_subdomain IS NULL OR public_subdomain NOT IN ('www', 'api', 'admin', 'auth', 'test', 'testing', 'qa', 'staging', 'localhost')
    ),
    ADD CONSTRAINT institutions_logo_metadata_check CHECK (
        (logo_key IS NULL AND logo_content_type IS NULL AND logo_version IS NULL AND logo_size IS NULL)
        OR (logo_key IS NOT NULL AND logo_content_type IS NOT NULL AND logo_content_type IN ('image/png', 'image/jpeg')
            AND logo_version IS NOT NULL AND logo_size IS NOT NULL AND logo_size BETWEEN 1 AND 2097152)
    );
