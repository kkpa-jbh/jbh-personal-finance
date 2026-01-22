-- =============================================
-- JBH Personal Finance - Database Schema
-- Schema: acctmgmt
-- Database: jbh_finance
-- Exported: 2025-12-15 06:16:00
-- =============================================

--
-- PostgreSQL database dump
--

-- Dumped from database version 16.9 (Homebrew)
-- Dumped by pg_dump version 16.9 (Homebrew)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: acctmgmt; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA acctmgmt;


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: account_monthly_balances; Type: TABLE; Schema: acctmgmt; Owner: -
--

CREATE TABLE acctmgmt.account_monthly_balances (
    id bigint NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    account_id uuid NOT NULL,
    year integer NOT NULL,
    month integer NOT NULL,
    period date NOT NULL,
    total_debits numeric(20,0) DEFAULT 0.00 NOT NULL,
    total_credits numeric(20,0) DEFAULT 0.00 NOT NULL,
    movement_balance numeric(20,0) DEFAULT 0.00 NOT NULL,
    opening_balance numeric(20,0) DEFAULT 0.00 NOT NULL,
    closing_balance numeric(20,0) DEFAULT 0.00 NOT NULL,
    monthly_reported_profit numeric(20,0),
    monthly_net_profit numeric(20,0) DEFAULT 0.00 NOT NULL,
    income_withholding_tax_amount numeric(20,0),
    net_growth_rate numeric(10,2) DEFAULT 0.00 NOT NULL,
    total_movements integer DEFAULT 0 NOT NULL,
    gap_period boolean DEFAULT false NOT NULL,
    official_monthly_report boolean DEFAULT false NOT NULL,
    CONSTRAINT chk_month_valid CHECK (((month >= 1) AND (month <= 12))),
    CONSTRAINT chk_period_first_day CHECK ((EXTRACT(day FROM period) = (1)::numeric)),
    CONSTRAINT chk_total_movements_positive CHECK ((total_movements >= 0)),
    CONSTRAINT chk_year_valid CHECK (((year >= 1900) AND (year <= 3100)))
);


--
-- Name: account_monthly_balances_id_seq; Type: SEQUENCE; Schema: acctmgmt; Owner: -
--

CREATE SEQUENCE acctmgmt.account_monthly_balances_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: account_monthly_balances_id_seq; Type: SEQUENCE OWNED BY; Schema: acctmgmt; Owner: -
--

ALTER SEQUENCE acctmgmt.account_monthly_balances_id_seq OWNED BY acctmgmt.account_monthly_balances.id;


--
-- Name: account_monthly_balances_seq; Type: SEQUENCE; Schema: acctmgmt; Owner: -
--

CREATE SEQUENCE acctmgmt.account_monthly_balances_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: account_movements; Type: TABLE; Schema: acctmgmt; Owner: -
--

CREATE TABLE acctmgmt.account_movements (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    account_id uuid NOT NULL,
    category_type text NOT NULL,
    movement_type text NOT NULL,
    movement_amount numeric(20,2) DEFAULT 0.00 NOT NULL,
    movement_date date NOT NULL,
    balance_snapshot numeric(20,2) DEFAULT 0.00 NOT NULL,
    description text,
    metadata jsonb,
    CONSTRAINT chk_balance_snapshot_valid CHECK ((balance_snapshot >= '-999999999999.99'::numeric)),
    CONSTRAINT chk_movement_date_not_future CHECK ((movement_date <= CURRENT_DATE))
);


--
-- Name: accounts; Type: TABLE; Schema: acctmgmt; Owner: -
--

CREATE TABLE acctmgmt.accounts (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    name text NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    type text,
    user_id uuid NOT NULL,
    movement_balance numeric(20,2) DEFAULT 0.00 NOT NULL,
    current_balance numeric(20,2) DEFAULT 0.00 NOT NULL,
    net_profit_balance numeric(20,2) DEFAULT 0.00 NOT NULL,
    net_growth_rate numeric(10,2) DEFAULT 0.00 NOT NULL,
    metadata jsonb,
    CONSTRAINT chk_current_balance_valid CHECK ((current_balance >= '-999999999.99'::numeric)),
    CONSTRAINT chk_name_not_empty CHECK ((length(TRIM(BOTH FROM name)) > 0))
);


--
-- Name: databasechangelog; Type: TABLE; Schema: acctmgmt; Owner: -
--

CREATE TABLE acctmgmt.databasechangelog (
    id character varying(255) NOT NULL,
    author character varying(255) NOT NULL,
    filename character varying(255) NOT NULL,
    dateexecuted timestamp without time zone NOT NULL,
    orderexecuted integer NOT NULL,
    exectype character varying(10) NOT NULL,
    md5sum character varying(35),
    description character varying(255),
    comments character varying(255),
    tag character varying(255),
    liquibase character varying(20),
    contexts character varying(255),
    labels character varying(255),
    deployment_id character varying(10)
);


--
-- Name: databasechangeloglock; Type: TABLE; Schema: acctmgmt; Owner: -
--

CREATE TABLE acctmgmt.databasechangeloglock (
    id integer NOT NULL,
    locked boolean NOT NULL,
    lockgranted timestamp without time zone,
    lockedby character varying(255)
);


--
-- Name: account_monthly_balances id; Type: DEFAULT; Schema: acctmgmt; Owner: -
--

ALTER TABLE ONLY acctmgmt.account_monthly_balances ALTER COLUMN id SET DEFAULT nextval('acctmgmt.account_monthly_balances_id_seq'::regclass);


--
-- Name: account_monthly_balances account_monthly_balances_pkey; Type: CONSTRAINT; Schema: acctmgmt; Owner: -
--

ALTER TABLE ONLY acctmgmt.account_monthly_balances
    ADD CONSTRAINT account_monthly_balances_pkey PRIMARY KEY (id);


--
-- Name: account_movements account_movements_pkey; Type: CONSTRAINT; Schema: acctmgmt; Owner: -
--

ALTER TABLE ONLY acctmgmt.account_movements
    ADD CONSTRAINT account_movements_pkey PRIMARY KEY (id);


--
-- Name: accounts accounts_pkey; Type: CONSTRAINT; Schema: acctmgmt; Owner: -
--

ALTER TABLE ONLY acctmgmt.accounts
    ADD CONSTRAINT accounts_pkey PRIMARY KEY (id);


--
-- Name: databasechangeloglock databasechangeloglock_pkey; Type: CONSTRAINT; Schema: acctmgmt; Owner: -
--

ALTER TABLE ONLY acctmgmt.databasechangeloglock
    ADD CONSTRAINT databasechangeloglock_pkey PRIMARY KEY (id);


--
-- Name: account_monthly_balances uk_account_monthly_balances_account_period; Type: CONSTRAINT; Schema: acctmgmt; Owner: -
--

ALTER TABLE ONLY acctmgmt.account_monthly_balances
    ADD CONSTRAINT uk_account_monthly_balances_account_period UNIQUE (account_id, period);


--
-- Name: account_monthly_balances uk_account_monthly_balances_account_yearmonth; Type: CONSTRAINT; Schema: acctmgmt; Owner: -
--

ALTER TABLE ONLY acctmgmt.account_monthly_balances
    ADD CONSTRAINT uk_account_monthly_balances_account_yearmonth UNIQUE (account_id, year, month);


--
-- Name: idx_account_monthly_balances_account_id; Type: INDEX; Schema: acctmgmt; Owner: -
--

CREATE INDEX idx_account_monthly_balances_account_id ON acctmgmt.account_monthly_balances USING btree (account_id);


--
-- Name: idx_account_monthly_balances_account_period; Type: INDEX; Schema: acctmgmt; Owner: -
--

CREATE INDEX idx_account_monthly_balances_account_period ON acctmgmt.account_monthly_balances USING btree (account_id, period DESC);


--
-- Name: idx_account_monthly_balances_gap_period; Type: INDEX; Schema: acctmgmt; Owner: -
--

CREATE INDEX idx_account_monthly_balances_gap_period ON acctmgmt.account_monthly_balances USING btree (account_id) WHERE (gap_period = true);


--
-- Name: idx_account_movements_account_date; Type: INDEX; Schema: acctmgmt; Owner: -
--

CREATE INDEX idx_account_movements_account_date ON acctmgmt.account_movements USING btree (account_id, movement_date DESC);


--
-- Name: idx_account_movements_account_date_type; Type: INDEX; Schema: acctmgmt; Owner: -
--

CREATE INDEX idx_account_movements_account_date_type ON acctmgmt.account_movements USING btree (account_id, movement_date DESC, movement_type);


--
-- Name: idx_accounts_user_id; Type: INDEX; Schema: acctmgmt; Owner: -
--

CREATE INDEX idx_accounts_user_id ON acctmgmt.accounts USING btree (user_id);


--
-- Name: account_monthly_balances fk_account_monthly_balances_account_id; Type: FK CONSTRAINT; Schema: acctmgmt; Owner: -
--

ALTER TABLE ONLY acctmgmt.account_monthly_balances
    ADD CONSTRAINT fk_account_monthly_balances_account_id FOREIGN KEY (account_id) REFERENCES acctmgmt.accounts(id) ON DELETE CASCADE;


--
-- Name: account_movements fk_account_movements_account_id; Type: FK CONSTRAINT; Schema: acctmgmt; Owner: -
--

ALTER TABLE ONLY acctmgmt.account_movements
    ADD CONSTRAINT fk_account_movements_account_id FOREIGN KEY (account_id) REFERENCES acctmgmt.accounts(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

