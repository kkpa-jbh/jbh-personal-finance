-- The real database gets these from jbh-deploy/initdb (outside Liquibase). Liquibase needs them first.
CREATE SCHEMA IF NOT EXISTS finance;
CREATE SCHEMA IF NOT EXISTS preferences;
CREATE EXTENSION IF NOT EXISTS pgcrypto;
