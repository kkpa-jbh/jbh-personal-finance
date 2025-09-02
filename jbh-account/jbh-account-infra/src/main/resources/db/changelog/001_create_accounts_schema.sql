CREATE TABLE acctmgmt.accounts (
    id SERIAL PRIMARY KEY,
    name TEXT,
    type TEXT,
    user_id INT NOT NULL,
    balance DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    effective_balance DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);