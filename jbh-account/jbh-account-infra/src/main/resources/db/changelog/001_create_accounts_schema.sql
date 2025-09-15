CREATE TABLE acctmgmt.accounts
(
    id               UUID PRIMARY KEY                  DEFAULT gen_random_uuid(),
    name             TEXT                     NOT NULL,
    is_active        BOOLEAN                  NOT NULL DEFAULT TRUE,
    type             TEXT,
    user_id          UUID                     NOT NULL,
    movement_balance DECIMAL(20, 2)           NOT NULL DEFAULT 0.00, -- Increased precision
    current_balance  DECIMAL(20, 2)           NOT NULL DEFAULT 0.00,
    profit_balance   DECIMAL(20, 2)           NOT NULL DEFAULT 0.00,
    metadata         JSONB,                                          -- Additional flexible data
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Add constraints
    CONSTRAINT chk_current_balance_valid CHECK (current_balance >= -999999999.99),
    CONSTRAINT chk_name_not_empty CHECK (length(trim(name)) > 0)
);

CREATE INDEX idx_accounts_user_id ON acctmgmt.accounts (user_id);

-- Main account movements table
CREATE TABLE acctmgmt.account_movements
(
    id               UUID PRIMARY KEY                  DEFAULT gen_random_uuid(),
    account_id       UUID                     NOT NULL,
    movement_type    TEXT                     NOT NULL,
    movement_amount  DECIMAL(20, 2)           NOT NULL DEFAULT 0.00,
    movement_date    DATE                     NOT NULL,
    balance_snapshot DECIMAL(20, 2)           NOT NULL DEFAULT 0.00,

    -- Additional useful columns
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    description      TEXT,  -- Optional description for the movement
    metadata         JSONB, -- Additional flexible data

    -- Constraints
    CONSTRAINT chk_balance_snapshot_valid CHECK (balance_snapshot >= -999999999999.99),
    CONSTRAINT chk_movement_date_not_future CHECK (movement_date <= CURRENT_DATE),

    -- Foreign key constraint
    CONSTRAINT fk_account_movements_account_id
        FOREIGN KEY (account_id) REFERENCES acctmgmt.accounts (id) ON DELETE CASCADE
);

CREATE INDEX idx_account_movements_account_date ON acctmgmt.account_movements
    (account_id, movement_date DESC);

-- 3. Alternative composite index with movement_type for filtered queries
CREATE INDEX idx_account_movements_account_date_type ON acctmgmt.account_movements
    (account_id, movement_date DESC, movement_type);


-- Monthly balances table
-- Create sequence for monthly balance ID
DROP SEQUENCE IF EXISTS acctmgmt.account_monthly_balances_seq;
CREATE SEQUENCE acctmgmt.account_monthly_balances_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
CREATE TABLE acctmgmt.account_monthly_balances
(
    id               BIGSERIAL PRIMARY KEY,
    account_id       UUID                     NOT NULL,
    year             INTEGER                  NOT NULL,
    month            INTEGER                  NOT NULL,
    period           DATE                     NOT NULL, -- First day of the month (YYYY-MM-01)

    total_debits     DECIMAL(20, 0)           NOT NULL DEFAULT 0.00,
    total_credits    DECIMAL(20, 0)           NOT NULL DEFAULT 0.00,
    movement_balance DECIMAL(20, 0)           NOT NULL DEFAULT 0.00,
    opening_balance  DECIMAL(20, 0)           NOT NULL DEFAULT 0.00,
    closing_balance  DECIMAL(20, 0)           NOT NULL DEFAULT 0.00,
    monthly_profit   DECIMAL(20, 0)           NOT NULL DEFAULT 0.00,

    total_movements  INTEGER                  NOT NULL DEFAULT 0,
    gap_period       BOOLEAN                  NOT NULL DEFAULT FALSE,

    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_month_valid CHECK (month BETWEEN 1 AND 12),
    CONSTRAINT chk_year_valid CHECK (year BETWEEN 1900 AND 3100),
    CONSTRAINT chk_total_movements_positive CHECK (total_movements >= 0),
    CONSTRAINT chk_period_first_day CHECK (EXTRACT(DAY FROM period) = 1),

    -- Unique constraint - one record per account per month
    CONSTRAINT uk_account_monthly_balances_account_yearmonth
        UNIQUE (account_id, year, month),

    -- Unique constraint - one record per account per month
    CONSTRAINT uk_account_monthly_balances_account_period
        UNIQUE (account_id, period),

    -- Foreign key
    CONSTRAINT fk_account_monthly_balances_account_id
        FOREIGN KEY (account_id) REFERENCES acctmgmt.accounts (id) ON DELETE CASCADE
);

-- Essential indexes
CREATE INDEX idx_account_monthly_balances_account_id ON acctmgmt.account_monthly_balances (account_id);
CREATE INDEX idx_account_monthly_balances_account_period ON acctmgmt.account_monthly_balances (account_id, period DESC);
CREATE INDEX idx_account_monthly_balances_gap_period ON acctmgmt.account_monthly_balances (account_id) WHERE gap_period = TRUE;