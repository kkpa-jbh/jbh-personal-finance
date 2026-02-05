--liquibase formatted sql

--changeset jbh:002_create_team_preferences_table

CREATE TABLE preferences.team_preferences
(
    team_id            UUID PRIMARY KEY,
    default_currency   VARCHAR(10)              NOT NULL DEFAULT 'COP',
    savings_goal       DECIMAL(20, 2)                    DEFAULT 0.00,
    metadata           JSONB,
    last_modified_by   UUID,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_currency_valid CHECK (default_currency IN ('COP', 'USD')),
    CONSTRAINT chk_savings_goal_non_negative CHECK (savings_goal IS NULL OR savings_goal >= 0)
);

CREATE INDEX idx_team_preferences_metadata ON preferences.team_preferences USING GIN (metadata);
CREATE INDEX idx_team_preferences_last_modified_by ON preferences.team_preferences (last_modified_by);

COMMENT ON COLUMN preferences.team_preferences.last_modified_by IS 'User ID who created or last modified the preferences';
