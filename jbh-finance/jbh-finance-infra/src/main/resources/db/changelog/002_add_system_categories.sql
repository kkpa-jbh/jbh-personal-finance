-- Replace category_type text column with category_id FK reference to finance.categories
ALTER TABLE finance.movements
    DROP COLUMN category_type;

ALTER TABLE finance.movements
    ADD COLUMN category_id INTEGER NULL;

ALTER TABLE finance.movements
    ADD CONSTRAINT fk_movements_category_id
        FOREIGN KEY (category_id) REFERENCES finance.categories (id);

-- Seed system categories (SystemCategoryAlias enum)
INSERT INTO finance.categories (source, alias, system, active, display_name)
VALUES
    -- INCOME SYSTEM CATEGORIES
    ('INCOME', 'INC_OTHER', TRUE, TRUE, '{"en": "Other",          "es": "Otro"}'),
    ('INCOME', 'INC_TRANSF', TRUE, TRUE, '{"en": "Transfer",       "es": "Transferencia"}'),
    ('INCOME', 'INC_DVDS', TRUE, TRUE, '{"en": "Dividends",      "es": "Dividendos"}'),
    -- Initial Balance is inactive because I do not want to expose it to the public.It's internal use
    ('INCOME', 'INC_INIT_BALANCE', TRUE, FALSE, '{"en": "Initial Balance","es": "Saldo Inicial"}'),
    -- EXPENSE SYSTEM CATEGORIES
    ('EXPENSE', 'EXP_TRANSF', TRUE, TRUE, '{"en": "Transfer",                  "es": "Transferencia"}'),
    ('EXPENSE', 'EXP_RTFTE', TRUE, TRUE, '{"en": "Withholding Tax",            "es": "Retención en la Fuente"}'),
    ('EXPENSE', 'INV_TO_CLOSE_IT', TRUE, TRUE, '{"en": "Investment Total Withdrawal","es": "Retiro Total de la Inversión"}'),
    ('EXPENSE', 'UNK', TRUE, false, '{"en": "Unknown",                    "es": "Desconocido"}')

-- INSERT NEW ONES HERE

--
ON CONFLICT (source, alias) DO NOTHING;
