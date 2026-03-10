--liquibase formatted sql

--changeset jbh:004_upgrade_category_id_to_bigint
-- Upgrade categories.id from integer (SERIAL) to bigint to match entity Long type
ALTER TABLE finance.categories ALTER COLUMN id SET DATA TYPE BIGINT;

-- Upgrade movements.category_id FK to match
ALTER TABLE finance.movements ALTER COLUMN category_id SET DATA TYPE BIGINT;
