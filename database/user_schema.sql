-- Day 4: users and account ownership.
-- Passwords must be stored as hashes only, never as plain text.

USE bank_demo;

CREATE TABLE app_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'ROLE_USER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (role IN ('ROLE_USER', 'ROLE_ADMIN'))
);

-- A user may own multiple accounts. Existing accounts are initially nullable
-- so that they can be assigned after a test user is registered.
ALTER TABLE accounts
    ADD COLUMN user_id BIGINT NULL,
    ADD CONSTRAINT fk_accounts_user
        FOREIGN KEY (user_id) REFERENCES app_users(id);
