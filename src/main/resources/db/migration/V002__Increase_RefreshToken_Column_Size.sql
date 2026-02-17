-- Flyway Migration: V002__Increase_RefreshToken_Column_Size.sql
-- Description: Increase refresh_tokens token column from VARCHAR(255) to VARCHAR(1000)
-- Reason: Encrypted JWT tokens are longer than 255 characters

ALTER TABLE refresh_tokens
ALTER COLUMN token TYPE VARCHAR(1000);