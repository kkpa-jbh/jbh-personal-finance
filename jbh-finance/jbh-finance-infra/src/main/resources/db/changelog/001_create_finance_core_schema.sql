-- financial instruments/accounts
CREATE TABLE finance.products
(
    id                 UUID PRIMARY KEY                  DEFAULT gen_random_uuid(),
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    name               TEXT                     NOT NULL,
    is_active          BOOLEAN                  NOT NULL DEFAULT TRUE,
    type               TEXT,
    user_id            UUID                     NOT NULL,
    movement_balance   DECIMAL(20, 2)           NOT NULL DEFAULT 0.00, -- Increased precision
    current_balance    DECIMAL(20, 2)           NOT NULL DEFAULT 0.00,
    net_profit_balance DECIMAL(20, 2)           NOT NULL DEFAULT 0.00,
    net_growth_rate    DECIMAL(10, 2)           NOT NULL DEFAULT 0.00,
    metadata           JSONB,                                          -- Additional flexible data

    -- Add constraints
    CONSTRAINT chk_current_balance_valid CHECK (current_balance >= -999999999.99),
    CONSTRAINT chk_name_not_empty CHECK (length(trim(name)) > 0)
);

CREATE INDEX idx_products_user_id ON finance.products (user_id);

-- Main product movements table
CREATE TABLE finance.movements
(
    id               UUID PRIMARY KEY                  DEFAULT gen_random_uuid(),
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    product_id       UUID                     NOT NULL,
    category_type    TEXT                     NULL,
    movement_type    TEXT                     NOT NULL,
    movement_amount  DECIMAL(20, 2)           NOT NULL DEFAULT 0.00,
    movement_date    DATE                     NOT NULL,
    balance_snapshot DECIMAL(20, 2)           NULL,

    -- Additional useful columns

    description      TEXT,  -- Optional description for the movement
    metadata         JSONB, -- Additional flexible data

    -- Constraints
    CONSTRAINT chk_balance_snapshot_valid CHECK (balance_snapshot >= -999999999999.99),
    CONSTRAINT chk_movement_date_not_future CHECK (movement_date <= CURRENT_DATE),

    -- Foreign key constraint
    CONSTRAINT fk_movements_product_id
        FOREIGN KEY (product_id) REFERENCES finance.products (id) ON DELETE CASCADE
);

CREATE INDEX idx_movements_product_date ON finance.movements
    (product_id, movement_date DESC);

-- 3. Alternative composite index with movement_type for filtered queries
CREATE INDEX idx_movements_product_date_type ON finance.movements
    (product_id, movement_date DESC, movement_type);


-- Monthly balances table
-- Create sequence for monthly balance ID
DROP SEQUENCE IF EXISTS finance.monthly_balances_seq;
CREATE SEQUENCE finance.monthly_balances_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
CREATE TABLE finance.monthly_balances
(
    id                            BIGSERIAL PRIMARY KEY,
    created_at                    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    product_id                    UUID                     NOT NULL,
    year                          INTEGER                  NOT NULL,
    month                         INTEGER                  NOT NULL,
    period                        DATE                     NOT NULL, -- First day of the month (YYYY-MM-01)

    total_debits                  DECIMAL(20, 2)           NOT NULL DEFAULT 0.00,
    total_credits                 DECIMAL(20, 2)           NOT NULL DEFAULT 0.00,
    movement_balance              DECIMAL(20, 2)           NOT NULL DEFAULT 0.00,
    opening_balance               DECIMAL(20, 2)           NOT NULL DEFAULT 0.00,
    closing_balance               DECIMAL(20, 2)           NOT NULL DEFAULT 0.00,
    monthly_reported_profit       DECIMAL(20, 2),
    monthly_net_profit            DECIMAL(20, 2)           NOT NULL DEFAULT 0.00,
    income_withholding_tax_amount DECIMAL(20, 2),
    net_growth_rate               DECIMAL(10, 2)           NOT NULL DEFAULT 0.00,

    total_movements               INTEGER                  NOT NULL DEFAULT 0,
    gap_period                    BOOLEAN                  NOT NULL DEFAULT FALSE,
    official_monthly_report       BOOLEAN                  NOT NULL DEFAULT FALSE,


    -- Constraints
    CONSTRAINT chk_month_valid CHECK (month BETWEEN 1 AND 12),
    CONSTRAINT chk_year_valid CHECK (year BETWEEN 1900 AND 3100),
    CONSTRAINT chk_total_movements_positive CHECK (total_movements >= 0),
    CONSTRAINT chk_period_first_day CHECK (EXTRACT(DAY FROM period) = 1),

    -- Unique constraint - one record per product per month
    CONSTRAINT uk_monthly_balances_product_yearmonth
        UNIQUE (product_id, year, month),

    -- Unique constraint - one record per product per month
    CONSTRAINT uk_monthly_balances_product_period
        UNIQUE (product_id, period),

    -- Foreign key
    CONSTRAINT fk_monthly_balances_product_id
        FOREIGN KEY (product_id) REFERENCES finance.products (id) ON DELETE CASCADE
);

-- Essential indexes
CREATE INDEX idx_monthly_balances_product_id ON finance.monthly_balances (product_id);
CREATE INDEX idx_monthly_balances_product_period ON finance.monthly_balances (product_id, period DESC);
CREATE INDEX idx_monthly_balances_gap_period ON finance.monthly_balances (product_id) WHERE gap_period = TRUE;


COMMENT ON TABLE finance.products IS
    'Financial instruments: bank accounts, investment accounts, credit cards';
COMMENT ON COLUMN finance.products.movement_balance IS
    'Sum of all movements (credits - debits)';