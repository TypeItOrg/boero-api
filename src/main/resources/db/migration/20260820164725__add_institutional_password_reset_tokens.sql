CREATE TABLE institutional_password_reset_tokens (
  password_reset_token_id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  token_hash VARCHAR(64) NOT NULL UNIQUE,
  expires_at TIMESTAMP NOT NULL,
  used_at TIMESTAMP,
  created_at TIMESTAMP NOT NULL
);

CREATE INDEX institutional_password_reset_tokens_user_id_idx
  ON institutional_password_reset_tokens(user_id);


