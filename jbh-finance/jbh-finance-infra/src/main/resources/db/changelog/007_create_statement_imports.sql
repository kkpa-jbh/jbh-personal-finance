-- One row per statement import attempt.
CREATE TABLE finance.statement_imports
(
    id          UUID PRIMARY KEY,
    user_id     UUID                     NOT NULL,
    product_id  UUID                     NOT NULL,
    pdf_sha256  TEXT                     NOT NULL,
    status      TEXT                     NOT NULL,
    error_code  TEXT                     NULL,
    summary     JSONB                    NULL,
    started_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP WITH TIME ZONE NULL,

    CONSTRAINT chk_statement_imports_status
        CHECK (status IN ('PROCESSING', 'DONE', 'FAILED')),
    CONSTRAINT chk_statement_imports_sha256
        CHECK (pdf_sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT chk_statement_imports_finished
        CHECK ((status = 'PROCESSING') = (finished_at IS NULL)),
    CONSTRAINT chk_statement_imports_error
        CHECK ((status = 'FAILED') = (error_code IS NOT NULL)),
    CONSTRAINT chk_statement_imports_summary
        CHECK (status <> 'DONE' OR summary IS NOT NULL),

    CONSTRAINT fk_statement_imports_product_id
        FOREIGN KEY (product_id) REFERENCES finance.products (id) ON DELETE CASCADE
);

-- The same PDF cannot be PROCESSING or DONE twice in one product. FAILED rows do not count.
CREATE UNIQUE INDEX uk_statement_imports_product_pdf_active
    ON finance.statement_imports (product_id, pdf_sha256)
    WHERE status <> 'FAILED';

-- Needed by the ON DELETE CASCADE from products (the partial index above skips FAILED rows).
CREATE INDEX idx_statement_imports_product_id ON finance.statement_imports (product_id);

-- Link from a movement to the import that created it.
ALTER TABLE finance.movements
    ADD COLUMN statement_import_id UUID NULL
        CONSTRAINT fk_movements_statement_import_id
            REFERENCES finance.statement_imports (id) ON DELETE SET NULL;

CREATE INDEX idx_movements_statement_import_date
    ON finance.movements (statement_import_id, movement_date)
    WHERE statement_import_id IS NOT NULL;

COMMENT ON TABLE finance.statement_imports IS
    'Bank statement import attempts. The PDF is never stored, only its SHA-256.';
