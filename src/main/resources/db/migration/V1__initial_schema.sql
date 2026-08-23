-- ============================================================
-- LUKU Boost - Electricity Loan Platform
-- Initial Database Schema
-- ============================================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================
-- CUSTOMERS
-- ============================================================
CREATE TABLE customers (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_ref    VARCHAR(30) NOT NULL,
    phone_number    VARCHAR(20) NOT NULL,
    first_name      VARCHAR(100),
    last_name       VARCHAR(100),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version         BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_customers_ref UNIQUE (customer_ref),
    CONSTRAINT uq_customers_phone UNIQUE (phone_number),
    CONSTRAINT chk_customers_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'BLOCKED'))
);

CREATE INDEX idx_customers_phone ON customers (phone_number);
CREATE INDEX idx_customers_status ON customers (status);

-- ============================================================
-- METERS
-- ============================================================
CREATE TABLE meters (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    meter_number    VARCHAR(30) NOT NULL,
    customer_id     UUID REFERENCES customers(id),
    meter_type      VARCHAR(30),
    provider        VARCHAR(50),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_token_amount DECIMAL(19,4),
    last_token_date TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version         BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_meters_number UNIQUE (meter_number),
    CONSTRAINT chk_meters_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'BLOCKED'))
);

CREATE INDEX idx_meters_number ON meters (meter_number);
CREATE INDEX idx_meters_customer ON meters (customer_id);

-- ============================================================
-- LOAN APPLICATIONS
-- ============================================================
CREATE TABLE loan_applications (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    application_ref     VARCHAR(40) NOT NULL,
    customer_id         UUID NOT NULL REFERENCES customers(id),
    meter_id            UUID NOT NULL REFERENCES meters(id),
    phone_number        VARCHAR(20) NOT NULL,
    meter_number        VARCHAR(30) NOT NULL,
    requested_amount    DECIMAL(19,4),
    eligible_amount     DECIMAL(19,4),
    approved_amount     DECIMAL(19,4),
    currency            VARCHAR(3) NOT NULL DEFAULT 'TZS',
    status              VARCHAR(30) NOT NULL DEFAULT 'INITIATED',
    eligibility_ref     VARCHAR(40),
    idempotency_key     VARCHAR(64),
    correlation_id      VARCHAR(64),
    failure_reason      TEXT,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version             BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_loan_app_ref UNIQUE (application_ref),
    CONSTRAINT uq_loan_app_idempotency UNIQUE (idempotency_key),
    CONSTRAINT chk_loan_app_status CHECK (status IN (
        'INITIATED', 'OTP_PENDING', 'OTP_VERIFIED', 'ELIGIBILITY_CHECKING',
        'ELIGIBLE', 'NOT_ELIGIBLE', 'AMOUNT_SELECTED', 'PROCESSING',
        'TOKEN_REQUESTED', 'TOKEN_RECEIVED', 'COMPLETED', 'FAILED',
        'TIMEOUT', 'UNKNOWN', 'CANCELLED', 'EXPIRED', 'REVERSED'
    ))
);

CREATE INDEX idx_loan_app_ref ON loan_applications (application_ref);
CREATE INDEX idx_loan_app_customer ON loan_applications (customer_id);
CREATE INDEX idx_loan_app_status ON loan_applications (status);
CREATE INDEX idx_loan_app_created ON loan_applications (created_at);

-- ============================================================
-- LOANS
-- ============================================================
CREATE TABLE loans (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    loan_ref            VARCHAR(40) NOT NULL,
    customer_id         UUID NOT NULL REFERENCES customers(id),
    meter_id            UUID NOT NULL REFERENCES meters(id),
    application_id      UUID NOT NULL REFERENCES loan_applications(id),
    principal_amount    DECIMAL(19,4) NOT NULL,
    fee_amount          DECIMAL(19,4) NOT NULL DEFAULT 0,
    total_due           DECIMAL(19,4) NOT NULL,
    outstanding_amount  DECIMAL(19,4) NOT NULL,
    currency            VARCHAR(3) NOT NULL DEFAULT 'TZS',
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    issued_at           TIMESTAMP WITH TIME ZONE,
    due_at              TIMESTAMP WITH TIME ZONE,
    completed_at        TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version             BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_loans_ref UNIQUE (loan_ref),
    CONSTRAINT uq_loans_application UNIQUE (application_id),
    CONSTRAINT chk_loans_status CHECK (status IN ('ACTIVE', 'COMPLETED', 'DEFAULTED', 'WRITTEN_OFF', 'REVERSED')),
    CONSTRAINT chk_loans_amounts CHECK (principal_amount > 0 AND total_due > 0 AND outstanding_amount >= 0)
);

CREATE INDEX idx_loans_ref ON loans (loan_ref);
CREATE INDEX idx_loans_customer ON loans (customer_id);
CREATE INDEX idx_loans_status ON loans (status);
CREATE INDEX idx_loans_due_at ON loans (due_at);
CREATE INDEX idx_loans_created ON loans (created_at);

-- ============================================================
-- LOAN TRANSACTIONS
-- ============================================================
CREATE TABLE loan_transactions (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    transaction_ref     VARCHAR(40) NOT NULL,
    loan_id             UUID NOT NULL REFERENCES loans(id),
    transaction_type    VARCHAR(30) NOT NULL,
    amount              DECIMAL(19,4) NOT NULL,
    currency            VARCHAR(3) NOT NULL DEFAULT 'TZS',
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    provider_ref        VARCHAR(100),
    idempotency_key     VARCHAR(64),
    description         TEXT,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_loan_txn_ref UNIQUE (transaction_ref),
    CONSTRAINT chk_loan_txn_type CHECK (transaction_type IN ('DISBURSEMENT', 'REPAYMENT', 'FEE', 'REVERSAL', 'ADJUSTMENT')),
    CONSTRAINT chk_loan_txn_status CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'REVERSED'))
);

CREATE INDEX idx_loan_txn_ref ON loan_transactions (transaction_ref);
CREATE INDEX idx_loan_txn_loan ON loan_transactions (loan_id);
CREATE INDEX idx_loan_txn_status ON loan_transactions (status);

-- ============================================================
-- ELECTRICITY TRANSACTIONS
-- ============================================================
CREATE TABLE electricity_transactions (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    internal_ref        VARCHAR(40) NOT NULL,
    provider_ref        VARCHAR(100),
    loan_id             UUID REFERENCES loans(id),
    application_id      UUID REFERENCES loan_applications(id),
    meter_number        VARCHAR(30) NOT NULL,
    amount              DECIMAL(19,4) NOT NULL,
    currency            VARCHAR(3) NOT NULL DEFAULT 'TZS',
    request_status      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    provider_status     VARCHAR(50),
    token               TEXT,
    token_units         VARCHAR(50),
    request_timestamp   TIMESTAMP WITH TIME ZONE,
    response_timestamp  TIMESTAMP WITH TIME ZONE,
    failure_reason      TEXT,
    retry_count         INTEGER NOT NULL DEFAULT 0,
    idempotency_key     VARCHAR(64),
    correlation_id      VARCHAR(64),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_elec_txn_ref UNIQUE (internal_ref),
    CONSTRAINT uq_elec_txn_idempotency UNIQUE (idempotency_key),
    CONSTRAINT chk_elec_txn_status CHECK (request_status IN ('PENDING', 'SENT', 'SUCCESS', 'FAILED', 'TIMEOUT', 'UNKNOWN'))
);

CREATE INDEX idx_elec_txn_ref ON electricity_transactions (internal_ref);
CREATE INDEX idx_elec_txn_provider_ref ON electricity_transactions (provider_ref);
CREATE INDEX idx_elec_txn_loan ON electricity_transactions (loan_id);
CREATE INDEX idx_elec_txn_meter ON electricity_transactions (meter_number);
CREATE INDEX idx_elec_txn_status ON electricity_transactions (request_status);
CREATE INDEX idx_elec_txn_created ON electricity_transactions (created_at);

-- ============================================================
-- OTP REQUESTS
-- ============================================================
CREATE TABLE otp_requests (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    phone_number        VARCHAR(20) NOT NULL,
    otp_hash            VARCHAR(128) NOT NULL,
    salt                VARCHAR(64) NOT NULL,
    purpose             VARCHAR(30) NOT NULL DEFAULT 'LOAN_APPLICATION',
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts_remaining  INTEGER NOT NULL DEFAULT 5,
    application_ref     VARCHAR(40),
    ip_address          VARCHAR(45),
    expires_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    verified_at         TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_otp_status CHECK (status IN ('PENDING', 'VERIFIED', 'EXPIRED', 'BLOCKED', 'SUPERSEDED'))
);

CREATE INDEX idx_otp_phone ON otp_requests (phone_number);
CREATE INDEX idx_otp_status ON otp_requests (status);
CREATE INDEX idx_otp_app_ref ON otp_requests (application_ref);
CREATE INDEX idx_otp_expires ON otp_requests (expires_at);

-- ============================================================
-- OTP VERIFICATIONS
-- ============================================================
CREATE TABLE otp_verifications (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    otp_request_id      UUID NOT NULL REFERENCES otp_requests(id),
    success             BOOLEAN NOT NULL,
    ip_address          VARCHAR(45),
    user_agent          TEXT,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_otp_verify_request ON otp_verifications (otp_request_id);

-- ============================================================
-- REPAYMENTS
-- ============================================================
CREATE TABLE repayments (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    repayment_ref       VARCHAR(40) NOT NULL,
    loan_id             UUID NOT NULL REFERENCES loans(id),
    amount              DECIMAL(19,4) NOT NULL,
    currency            VARCHAR(3) NOT NULL DEFAULT 'TZS',
    payment_method      VARCHAR(30),
    provider_ref        VARCHAR(100),
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    idempotency_key     VARCHAR(64),
    transaction_date    TIMESTAMP WITH TIME ZONE,
    failure_reason      TEXT,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_repayments_ref UNIQUE (repayment_ref),
    CONSTRAINT uq_repayments_idempotency UNIQUE (idempotency_key),
    CONSTRAINT chk_repayments_status CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'REVERSED')),
    CONSTRAINT chk_repayments_amount CHECK (amount > 0)
);

CREATE INDEX idx_repayments_ref ON repayments (repayment_ref);
CREATE INDEX idx_repayments_loan ON repayments (loan_id);
CREATE INDEX idx_repayments_status ON repayments (status);

-- ============================================================
-- ADMIN USERS
-- ============================================================
CREATE TABLE admin_users (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username        VARCHAR(50) NOT NULL,
    email           VARCHAR(100) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(150) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_login_at   TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version         BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_admin_username UNIQUE (username),
    CONSTRAINT uq_admin_email UNIQUE (email),
    CONSTRAINT chk_admin_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'LOCKED'))
);

-- ============================================================
-- ROLES
-- ============================================================
CREATE TABLE roles (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name            VARCHAR(50) NOT NULL,
    description     TEXT,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_roles_name UNIQUE (name),
    CONSTRAINT chk_roles_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

-- ============================================================
-- PERMISSIONS
-- ============================================================
CREATE TABLE permissions (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name            VARCHAR(50) NOT NULL,
    description     TEXT,
    module          VARCHAR(30),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_permissions_name UNIQUE (name)
);

-- ============================================================
-- ROLE PERMISSIONS
-- ============================================================
CREATE TABLE role_permissions (
    role_id         UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id   UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- ============================================================
-- ADMIN USER ROLES
-- ============================================================
CREATE TABLE admin_user_roles (
    user_id         UUID NOT NULL REFERENCES admin_users(id) ON DELETE CASCADE,
    role_id         UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- ============================================================
-- AUDIT LOGS (append-only)
-- ============================================================
CREATE TABLE audit_logs (
    id              BIGSERIAL PRIMARY KEY,
    user_id         UUID,
    user_type       VARCHAR(20) NOT NULL DEFAULT 'SYSTEM',
    username        VARCHAR(100),
    action          VARCHAR(50) NOT NULL,
    entity_type     VARCHAR(50),
    entity_id       VARCHAR(64),
    old_value       JSONB,
    new_value       JSONB,
    description     TEXT,
    ip_address      VARCHAR(45),
    user_agent      TEXT,
    correlation_id  VARCHAR(64),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_audit_user_type CHECK (user_type IN ('ADMIN', 'CUSTOMER', 'SYSTEM'))
);

CREATE INDEX idx_audit_user ON audit_logs (user_id);
CREATE INDEX idx_audit_action ON audit_logs (action);
CREATE INDEX idx_audit_entity ON audit_logs (entity_type, entity_id);
CREATE INDEX idx_audit_created ON audit_logs (created_at);
CREATE INDEX idx_audit_correlation ON audit_logs (correlation_id);

-- ============================================================
-- NOTIFICATIONS
-- ============================================================
CREATE TABLE notifications (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    recipient       VARCHAR(100) NOT NULL,
    channel         VARCHAR(20) NOT NULL,
    template        VARCHAR(50),
    subject         VARCHAR(200),
    content         TEXT NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count     INTEGER NOT NULL DEFAULT 0,
    max_retries     INTEGER NOT NULL DEFAULT 3,
    provider_ref    VARCHAR(100),
    failure_reason  TEXT,
    correlation_id  VARCHAR(64),
    sent_at         TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_notif_channel CHECK (channel IN ('SMS', 'EMAIL', 'PUSH')),
    CONSTRAINT chk_notif_status CHECK (status IN ('PENDING', 'SENT', 'DELIVERED', 'FAILED'))
);

CREATE INDEX idx_notif_recipient ON notifications (recipient);
CREATE INDEX idx_notif_status ON notifications (status);
CREATE INDEX idx_notif_created ON notifications (created_at);

-- ============================================================
-- SYSTEM CONFIGURATIONS
-- ============================================================
CREATE TABLE system_configurations (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    config_key      VARCHAR(100) NOT NULL,
    config_value    TEXT NOT NULL,
    category        VARCHAR(50) NOT NULL,
    description     TEXT,
    value_type      VARCHAR(20) NOT NULL DEFAULT 'STRING',
    is_sensitive    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_by      UUID,
    version         BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_sys_config_key UNIQUE (config_key),
    CONSTRAINT chk_sys_value_type CHECK (value_type IN ('STRING', 'INTEGER', 'DECIMAL', 'BOOLEAN', 'JSON'))
);

CREATE INDEX idx_sys_config_category ON system_configurations (category);

-- ============================================================
-- LOAN RULE CONFIGURATIONS
-- ============================================================
CREATE TABLE loan_rule_configurations (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    rule_name       VARCHAR(100) NOT NULL,
    rule_type       VARCHAR(50) NOT NULL,
    rule_value      JSONB NOT NULL,
    priority        INTEGER NOT NULL DEFAULT 0,
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    description     TEXT,
    effective_from  TIMESTAMP WITH TIME ZONE,
    effective_to    TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_by      UUID,
    version         BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_loan_rule_name UNIQUE (rule_name)
);

CREATE INDEX idx_loan_rule_type ON loan_rule_configurations (rule_type);
CREATE INDEX idx_loan_rule_enabled ON loan_rule_configurations (enabled);

-- ============================================================
-- RECONCILIATION RECORDS
-- ============================================================
CREATE TABLE reconciliation_records (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    recon_ref           VARCHAR(40) NOT NULL,
    internal_ref        VARCHAR(40) NOT NULL,
    provider_ref        VARCHAR(100),
    internal_status     VARCHAR(20) NOT NULL,
    provider_status     VARCHAR(50),
    internal_amount     DECIMAL(19,4),
    provider_amount     DECIMAL(19,4),
    mismatch_type       VARCHAR(50) NOT NULL,
    resolution_status   VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    resolution_notes    TEXT,
    resolved_by         UUID,
    resolved_at         TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_recon_ref UNIQUE (recon_ref),
    CONSTRAINT chk_recon_mismatch CHECK (mismatch_type IN ('STATUS_MISMATCH', 'AMOUNT_MISMATCH', 'MISSING_INTERNAL', 'MISSING_PROVIDER', 'TIMEOUT_UNRESOLVED')),
    CONSTRAINT chk_recon_resolution CHECK (resolution_status IN ('PENDING', 'INVESTIGATING', 'RESOLVED', 'ESCALATED'))
);

CREATE INDEX idx_recon_ref ON reconciliation_records (recon_ref);
CREATE INDEX idx_recon_internal_ref ON reconciliation_records (internal_ref);
CREATE INDEX idx_recon_resolution ON reconciliation_records (resolution_status);
CREATE INDEX idx_recon_created ON reconciliation_records (created_at);

-- ============================================================
-- IDEMPOTENCY RECORDS
-- ============================================================
CREATE TABLE idempotency_records (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    idempotency_key VARCHAR(64) NOT NULL,
    operation       VARCHAR(50) NOT NULL,
    request_hash    VARCHAR(128),
    response        JSONB,
    status          VARCHAR(20) NOT NULL DEFAULT 'PROCESSING',
    expires_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_idempotency_key_op UNIQUE (idempotency_key, operation),
    CONSTRAINT chk_idempotency_status CHECK (status IN ('PROCESSING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX idx_idempotency_key ON idempotency_records (idempotency_key);
CREATE INDEX idx_idempotency_expires ON idempotency_records (expires_at);

-- ============================================================
-- PROVIDER REQUEST LOGS
-- ============================================================
CREATE TABLE provider_request_logs (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    correlation_id  VARCHAR(64),
    provider        VARCHAR(50) NOT NULL,
    operation       VARCHAR(50) NOT NULL,
    request_url     VARCHAR(500),
    request_method  VARCHAR(10),
    request_headers JSONB,
    request_body    TEXT,
    response_status INTEGER,
    response_body   TEXT,
    latency_ms      BIGINT,
    error_message   TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_provider_log_correlation ON provider_request_logs (correlation_id);
CREATE INDEX idx_provider_log_provider ON provider_request_logs (provider);
CREATE INDEX idx_provider_log_created ON provider_request_logs (created_at);

-- ============================================================
-- FAILED TRANSACTIONS
-- ============================================================
CREATE TABLE failed_transactions (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    original_ref    VARCHAR(40) NOT NULL,
    transaction_type VARCHAR(30) NOT NULL,
    entity_type     VARCHAR(30),
    entity_id       UUID,
    failure_reason  TEXT NOT NULL,
    retry_count     INTEGER NOT NULL DEFAULT 0,
    max_retries     INTEGER NOT NULL DEFAULT 3,
    last_retry_at   TIMESTAMP WITH TIME ZONE,
    next_retry_at   TIMESTAMP WITH TIME ZONE,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    resolution_notes TEXT,
    resolved_by     UUID,
    resolved_at     TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_failed_txn_status CHECK (status IN ('PENDING', 'RETRYING', 'RESOLVED', 'ESCALATED', 'ABANDONED'))
);

CREATE INDEX idx_failed_txn_ref ON failed_transactions (original_ref);
CREATE INDEX idx_failed_txn_status ON failed_transactions (status);
CREATE INDEX idx_failed_txn_next_retry ON failed_transactions (next_retry_at);
