ALTER TABLE users
  ADD COLUMN webauthn_user_handle BYTEA UNIQUE;

CREATE TABLE passkey_credentials (
  passkey_credential_id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  credential_id VARCHAR(255) NOT NULL UNIQUE,
  public_key_cose BYTEA NOT NULL,
  signature_count BIGINT NOT NULL DEFAULT 0,
  transports VARCHAR(255),
  backup_eligible BOOLEAN NOT NULL DEFAULT FALSE,
  backup_state BOOLEAN NOT NULL DEFAULT FALSE,
  uv_initialized BOOLEAN NOT NULL DEFAULT FALSE,
  user_handle BYTEA NOT NULL,
  attestation_object BYTEA,
  attestation_client_data_json BYTEA,
  label VARCHAR(100) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  last_used_at TIMESTAMP,
  revoked_at TIMESTAMP,
  CONSTRAINT passkey_credentials_label_length CHECK (char_length(label) >= 1 AND char_length(label) <= 100)
);

CREATE INDEX passkey_credentials_user_active_idx
  ON passkey_credentials(user_id)
  WHERE revoked_at IS NULL;

CREATE INDEX passkey_credentials_user_id_idx
  ON passkey_credentials(user_id);
