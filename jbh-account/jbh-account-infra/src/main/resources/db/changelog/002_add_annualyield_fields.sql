ALTER TABLE acctmgmt.account_monthly_balances
    ADD COLUMN official_monthly_report BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN estimated_annual_yield  DECIMAL(5, 2); -- snake_case for PostgreSQL


