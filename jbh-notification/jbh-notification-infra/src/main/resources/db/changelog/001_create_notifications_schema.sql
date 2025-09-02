-- liquibase formatted sql

-- changeset author:accounts-team id:001-create-accounts-schema
CREATE SCHEMA IF NOT EXISTS notifications;

-- rollback DROP SCHEMA IF EXISTS accounts CASCADE;