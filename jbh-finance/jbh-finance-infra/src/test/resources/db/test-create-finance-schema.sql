-- Real databases get this schema from an init script outside Liquibase. Liquibase needs it first.
CREATE SCHEMA IF NOT EXISTS finance;

-- Production sets currentSchema=finance in the JDBC URL. Hibernate needs it to find
-- monthly_balances_seq, which the entity names without a schema. This script runs as the same user
-- the pool connects with, before the pool opens, so every app connection starts in finance.
ALTER ROLE CURRENT_USER SET search_path TO finance, public;
