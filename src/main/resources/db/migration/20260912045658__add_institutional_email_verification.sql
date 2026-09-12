ALTER TABLE users
    ADD COLUMN email_verification_status varchar(24) NOT NULL DEFAULT 'NOT_REQUIRED',
    ADD COLUMN email_verified_at timestamptz,
    ADD CONSTRAINT users_email_verification_state CHECK (
        (email_verification_status IN ('NOT_REQUIRED', 'PENDING') AND email_verified_at IS NULL)
        OR (email_verification_status = 'VERIFIED' AND email_verified_at IS NOT NULL)
    );

CREATE TABLE institutional_email_verification_tokens (
    email_verification_token_id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES users(user_id),
    token_hash varchar(64) NOT NULL,
    recipient_email varchar(150) NOT NULL,
    issued_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL,
    used_at timestamptz,
    CONSTRAINT email_verification_user_unique UNIQUE (user_id),
    CONSTRAINT email_verification_hash_unique UNIQUE (token_hash),
    CONSTRAINT email_verification_expiration CHECK (expires_at > issued_at)
);
