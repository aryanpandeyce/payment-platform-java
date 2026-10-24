-- V3: Add owner_user_id column for Role-Based Access Control (RBAC)
-- Invariant FR-21: Nullable for backward compatibility with existing accounts
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS owner_user_id UUID;
CREATE INDEX IF NOT EXISTS idx_accounts_owner_user_id ON accounts(owner_user_id);
