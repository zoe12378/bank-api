-- Core schema for the Bank API.
-- Monetary values use DECIMAL(19, 2), never FLOAT or REAL.

CREATE DATABASE IF NOT EXISTS bank_demo
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE bank_demo;

CREATE TABLE accounts (
    account_number VARCHAR(20) PRIMARY KEY,
    owner_name VARCHAR(100) NOT NULL,
    balance DECIMAL(19, 2) NOT NULL CHECK (balance >= 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE account_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_number VARCHAR(20) NOT NULL REFERENCES accounts(account_number),
    transaction_type VARCHAR(20) NOT NULL CHECK (
        transaction_type IN ('OPEN_ACCOUNT', 'DEPOSIT', 'WITHDRAW', 'TRANSFER_OUT', 'TRANSFER_IN')
    ),
    status VARCHAR(10) NOT NULL CHECK (status IN ('SUCCESS', 'FAILED')),
    amount DECIMAL(19, 2) NOT NULL CHECK (amount > 0),
    balance_after DECIMAL(19, 2) NOT NULL CHECK (balance_after >= 0),
    counterparty_account_number VARCHAR(20),
    failure_reason VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_account_transactions_account_created_at
    ON account_transactions(account_number, created_at DESC);

-- Sample data for local development.
INSERT INTO accounts (account_number, owner_name, balance)
VALUES
    ('A001', 'Alice', 1150.00),
    ('B001', 'Bob', 500.00);

INSERT INTO account_transactions (
    account_number,
    transaction_type,
    status,
    amount,
    balance_after,
    counterparty_account_number,
    failure_reason
)
VALUES
    ('A001', 'OPEN_ACCOUNT', 'SUCCESS', 1000.00, 1000.00, NULL, NULL),
    ('A001', 'DEPOSIT', 'SUCCESS', 500.00, 1500.00, NULL, NULL),
    ('A001', 'TRANSFER_OUT', 'SUCCESS', 300.00, 1200.00, 'B001', NULL),
    ('A001', 'TRANSFER_OUT', 'SUCCESS', 50.00, 1150.00, 'B001', NULL),
    ('A001', 'WITHDRAW', 'FAILED', 2000.00, 1150.00, NULL, 'Insufficient balance'),
    ('B001', 'OPEN_ACCOUNT', 'SUCCESS', 250.00, 250.00, NULL, NULL),
    ('B001', 'TRANSFER_IN', 'SUCCESS', 300.00, 550.00, 'A001', NULL),
    ('B001', 'WITHDRAW', 'SUCCESS', 100.00, 450.00, NULL, NULL),
    ('B001', 'TRANSFER_IN', 'SUCCESS', 50.00, 500.00, 'A001', NULL);
