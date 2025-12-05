-- V4__token_credentials.sql
-- Stores encrypted OAuth tokens keyed by logical account id.

CREATE TABLE IF NOT EXISTS token_credentials (
    account_id VARCHAR(64) PRIMARY KEY,
    token      VARCHAR(4096) NOT NULL
);
