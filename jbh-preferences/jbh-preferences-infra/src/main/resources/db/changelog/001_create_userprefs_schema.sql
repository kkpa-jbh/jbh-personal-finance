--liquibase formatted sql

--changeset jbh:001_create_preferences_schema

CREATE SCHEMA IF NOT EXISTS preferences;

CREATE TABLE preferences.user_preferences
(
    user_id            UUID PRIMARY KEY,
    default_lang       VARCHAR(10)              NOT NULL DEFAULT 'en',
    default_currency   VARCHAR(10)              NOT NULL DEFAULT 'USD',
    savings_goal       DECIMAL(20, 2)                    DEFAULT 0.00,
    default_product_id UUID,
    metadata           JSONB,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_currency_valid CHECK (default_currency IN ('COP', 'USD')),
    CONSTRAINT chk_savings_goal_non_negative CHECK (savings_goal IS NULL OR savings_goal >= 0)
);

CREATE INDEX idx_user_preferences_metadata ON preferences.user_preferences USING GIN (metadata);
