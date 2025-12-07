-- V1__init_schema.sql
-- Initial database schema for StreamNotifier Core Service

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Create ENUM types
CREATE TYPE provider_type AS ENUM ('TELEGRAM');
CREATE TYPE platform_type AS ENUM ('YOUTUBE', 'TWITCH');

-- Table: accounts
CREATE TABLE accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Table: connections
CREATE TABLE connections (
    id BIGSERIAL PRIMARY KEY,
    account_id UUID NOT NULL,
    provider provider_type NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    CONSTRAINT fk_connections_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE,
    CONSTRAINT uk_connections_provider_provider_id UNIQUE (provider, provider_id)
);

-- Table: subscriptions
CREATE TABLE subscriptions (
    id BIGSERIAL PRIMARY KEY,
    account_id UUID NOT NULL,
    platform platform_type NOT NULL,
    channel_url VARCHAR(500) NOT NULL,
    channel_name VARCHAR(255),
    CONSTRAINT fk_subscriptions_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE,
    CONSTRAINT uk_subscriptions_account_channel UNIQUE (account_id, channel_url)
);

-- Create indexes for better query performance
CREATE INDEX idx_connections_account_id ON connections(account_id);
CREATE INDEX idx_subscriptions_account_id ON subscriptions(account_id);
