-- Run once for an existing bank_demo database after user_schema.sql.
-- Only the SHA-256 hash is stored; the raw refresh token is never saved in MySQL.

USE bank_demo;

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash CHAR(64) NOT NULL UNIQUE,
    expires_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at DATETIME NULL,
    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id) REFERENCES app_users(id)
);

CREATE INDEX idx_refresh_tokens_active
    ON refresh_tokens(token_hash, revoked_at, expires_at);
