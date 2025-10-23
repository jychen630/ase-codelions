-- V2__tweets.sql
-- Minimal tweets table used by DB-backed search

CREATE TABLE IF NOT EXISTS tweets (
  id          VARCHAR(64)   PRIMARY KEY,
  account_id  VARCHAR(128)  NOT NULL,
  user_handle VARCHAR(64)   NOT NULL,
  text        VARCHAR(1000) NOT NULL,
  created_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_tweets_account_created
  ON tweets (account_id, created_at DESC);

-- Simple text index. In Postgres you'd use GIN/tsvector; with H2 in PG mode a basic index is fine.
CREATE INDEX IF NOT EXISTS idx_tweets_text
  ON tweets (text);
