-- Categories catalog table
CREATE TABLE finance.categories
(
    id           SERIAL                   NOT NULL,
    source       TEXT                     NOT NULL,
    alias        VARCHAR(50)              NOT NULL,
    system       BOOLEAN                  NOT NULL DEFAULT FALSE,
    active       BOOLEAN                  NOT NULL DEFAULT TRUE,
    display_name JSONB,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_categories PRIMARY KEY (id),
    CONSTRAINT chk_category_source CHECK (source IN ('INCOME', 'EXPENSE')),
    CONSTRAINT chk_category_short_name_length CHECK (length(alias) <= 50),
    CONSTRAINT uk_category_source_name UNIQUE (source, alias)
);

CREATE INDEX idx_categories_source ON finance.categories (source);

COMMENT ON TABLE finance.categories IS
    'Category catalog for classifying financial movements by source (INCOME or EXPENSE)';
COMMENT ON COLUMN finance.categories.source IS
    'Movement source type: INCOME or EXPENSE';
COMMENT ON COLUMN finance.categories.alias IS
    'Unique category identifier within a source, max 50 characters';
COMMENT ON COLUMN finance.categories.display_name IS
    'Localized display names as a JSON map (e.g., {"en": "Housing", "es": "Vivienda"})';
