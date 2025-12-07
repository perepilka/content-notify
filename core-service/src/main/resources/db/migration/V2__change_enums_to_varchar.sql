-- V2__change_enums_to_varchar.sql
-- Change custom ENUM types to VARCHAR for Hibernate compatibility

-- Drop existing indexes temporarily
DROP INDEX IF EXISTS idx_connections_account_id;
DROP INDEX IF EXISTS idx_subscriptions_account_id;

-- Alter connections table
ALTER TABLE connections ALTER COLUMN provider TYPE VARCHAR(50);

-- Alter subscriptions table
ALTER TABLE subscriptions ALTER COLUMN platform TYPE VARCHAR(50);

-- Drop unused ENUM types
DROP TYPE IF EXISTS provider_type;
DROP TYPE IF EXISTS platform_type;

-- Recreate indexes
CREATE INDEX idx_connections_account_id ON connections(account_id);
CREATE INDEX idx_subscriptions_account_id ON subscriptions(account_id);
