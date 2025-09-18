ALTER TABLE acctmgmt.account_monthly_balances
    ADD COLUMN official_monthly_report BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN estimated_annual_yield  DECIMAL(5, 2); -- snake_case for PostgreSQL

ALTER TABLE acctmgmt.accounts
    ADD COLUMN advertised_annual_rate DECIMAL(5, 2), -- snake_case for PostgreSQL
    ADD COLUMN estimated_annual_yield DECIMAL(5, 2); -- snake_case for PostgreSQL

