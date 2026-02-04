--liquibase formatted sql

--changeset jbh:001_create_userprefs_schema

CREATE SCHEMA IF NOT EXISTS userprefs;

CREATE TABLE userprefs.user_preferences
(
    id                 UUID PRIMARY KEY                  DEFAULT gen_random_uuid(),
    user_id            UUID                     NOT NULL UNIQUE,
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

CREATE INDEX idx_user_preferences_user_id ON userprefs.user_preferences (user_id);
CREATE INDEX idx_user_preferences_metadata ON userprefs.user_preferences USING GIN (metadata);
