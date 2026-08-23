-- ============================================================
-- Seed Roles
-- ============================================================
INSERT INTO roles (id, name, description) VALUES
    ('a0000000-0000-0000-0000-000000000001', 'SUPER_ADMIN', 'Full system access'),
    ('a0000000-0000-0000-0000-000000000002', 'ADMIN', 'Administrative access'),
    ('a0000000-0000-0000-0000-000000000003', 'OPERATIONS', 'Operations team access'),
    ('a0000000-0000-0000-0000-000000000004', 'FINANCE', 'Finance team access'),
    ('a0000000-0000-0000-0000-000000000005', 'SUPPORT', 'Customer support access'),
    ('a0000000-0000-0000-0000-000000000006', 'AUDITOR', 'Audit and compliance access'),
    ('a0000000-0000-0000-0000-000000000007', 'READ_ONLY', 'Read-only access');

-- ============================================================
-- Seed Permissions
-- ============================================================
INSERT INTO permissions (id, name, description, module) VALUES
    ('b0000000-0000-0000-0000-000000000001', 'CUSTOMER_VIEW', 'View customer information', 'CUSTOMER'),
    ('b0000000-0000-0000-0000-000000000002', 'CUSTOMER_UPDATE', 'Update customer information', 'CUSTOMER'),
    ('b0000000-0000-0000-0000-000000000003', 'LOAN_VIEW', 'View loan information', 'LOAN'),
    ('b0000000-0000-0000-0000-000000000004', 'LOAN_MANAGE', 'Manage loans', 'LOAN'),
    ('b0000000-0000-0000-0000-000000000005', 'LOAN_RETRY', 'Retry failed loan operations', 'LOAN'),
    ('b0000000-0000-0000-0000-000000000006', 'TRANSACTION_VIEW', 'View transactions', 'TRANSACTION'),
    ('b0000000-0000-0000-0000-000000000007', 'TRANSACTION_RETRY', 'Retry failed transactions', 'TRANSACTION'),
    ('b0000000-0000-0000-0000-000000000008', 'RECONCILIATION_VIEW', 'View reconciliation records', 'RECONCILIATION'),
    ('b0000000-0000-0000-0000-000000000009', 'RECONCILIATION_MANAGE', 'Manage reconciliation', 'RECONCILIATION'),
    ('b0000000-0000-0000-0000-000000000010', 'CONFIG_VIEW', 'View system configuration', 'CONFIG'),
    ('b0000000-0000-0000-0000-000000000011', 'CONFIG_UPDATE', 'Update system configuration', 'CONFIG'),
    ('b0000000-0000-0000-0000-000000000012', 'USER_VIEW', 'View admin users', 'USER'),
    ('b0000000-0000-0000-0000-000000000013', 'USER_MANAGE', 'Manage admin users', 'USER'),
    ('b0000000-0000-0000-0000-000000000014', 'ROLE_VIEW', 'View roles', 'ROLE'),
    ('b0000000-0000-0000-0000-000000000015', 'ROLE_MANAGE', 'Manage roles and permissions', 'ROLE'),
    ('b0000000-0000-0000-0000-000000000016', 'AUDIT_VIEW', 'View audit logs', 'AUDIT'),
    ('b0000000-0000-0000-0000-000000000017', 'REPORT_VIEW', 'View and export reports', 'REPORT'),
    ('b0000000-0000-0000-0000-000000000018', 'REPORT_EXPORT', 'Export reports', 'REPORT'),
    ('b0000000-0000-0000-0000-000000000019', 'NOTIFICATION_VIEW', 'View notifications', 'NOTIFICATION'),
    ('b0000000-0000-0000-0000-000000000020', 'METER_VIEW', 'View meter information', 'METER'),
    ('b0000000-0000-0000-0000-000000000021', 'METER_MANAGE', 'Manage meters', 'METER'),
    ('b0000000-0000-0000-0000-000000000022', 'OTP_VIEW', 'View OTP monitoring data', 'OTP'),
    ('b0000000-0000-0000-0000-000000000023', 'PROVIDER_VIEW', 'View provider integration data', 'PROVIDER'),
    ('b0000000-0000-0000-0000-000000000024', 'REPAYMENT_VIEW', 'View repayments', 'REPAYMENT'),
    ('b0000000-0000-0000-0000-000000000025', 'REPAYMENT_MANAGE', 'Manage repayments', 'REPAYMENT');

-- ============================================================
-- Assign all permissions to SUPER_ADMIN
-- ============================================================
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'a0000000-0000-0000-0000-000000000001', id FROM permissions;

-- ADMIN gets most permissions except USER_MANAGE and ROLE_MANAGE
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'a0000000-0000-0000-0000-000000000002', id FROM permissions
WHERE name NOT IN ('USER_MANAGE', 'ROLE_MANAGE');

-- OPERATIONS
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'a0000000-0000-0000-0000-000000000003', id FROM permissions
WHERE name IN ('CUSTOMER_VIEW', 'LOAN_VIEW', 'LOAN_MANAGE', 'LOAN_RETRY',
    'TRANSACTION_VIEW', 'TRANSACTION_RETRY', 'RECONCILIATION_VIEW', 'RECONCILIATION_MANAGE',
    'METER_VIEW', 'OTP_VIEW', 'PROVIDER_VIEW', 'NOTIFICATION_VIEW', 'REPAYMENT_VIEW');

-- FINANCE
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'a0000000-0000-0000-0000-000000000004', id FROM permissions
WHERE name IN ('LOAN_VIEW', 'TRANSACTION_VIEW', 'RECONCILIATION_VIEW', 'RECONCILIATION_MANAGE',
    'REPORT_VIEW', 'REPORT_EXPORT', 'REPAYMENT_VIEW', 'REPAYMENT_MANAGE');

-- SUPPORT
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'a0000000-0000-0000-0000-000000000005', id FROM permissions
WHERE name IN ('CUSTOMER_VIEW', 'CUSTOMER_UPDATE', 'LOAN_VIEW', 'TRANSACTION_VIEW',
    'METER_VIEW', 'OTP_VIEW', 'NOTIFICATION_VIEW', 'REPAYMENT_VIEW');

-- AUDITOR
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'a0000000-0000-0000-0000-000000000006', id FROM permissions
WHERE name IN ('AUDIT_VIEW', 'REPORT_VIEW', 'REPORT_EXPORT', 'LOAN_VIEW',
    'TRANSACTION_VIEW', 'RECONCILIATION_VIEW', 'CUSTOMER_VIEW');

-- READ_ONLY
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'a0000000-0000-0000-0000-000000000007', id FROM permissions
WHERE name LIKE '%_VIEW';

-- ============================================================
-- Default Super Admin (password: Admin@2026!)
-- BCrypt hash of 'Admin@2026!'
-- ============================================================
INSERT INTO admin_users (id, username, email, password_hash, full_name, status) VALUES
    ('c0000000-0000-0000-0000-000000000001', 'superadmin', 'admin@lukuboost.com',
     '$2a$12$LJ3m4ys3uz0b9xNN0PiXbu2TbBz3PkMqbEyGECyRd6hl3qEMYFaHa',
     'System Administrator', 'ACTIVE');

INSERT INTO admin_user_roles (user_id, role_id) VALUES
    ('c0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001');

-- ============================================================
-- Seed System Configurations
-- ============================================================
INSERT INTO system_configurations (config_key, config_value, category, description, value_type) VALUES
    ('otp.length', '6', 'OTP', 'OTP code length', 'INTEGER'),
    ('otp.expiry.seconds', '300', 'OTP', 'OTP expiration in seconds', 'INTEGER'),
    ('otp.max.attempts', '5', 'OTP', 'Maximum OTP verification attempts', 'INTEGER'),
    ('otp.resend.cooldown.seconds', '60', 'OTP', 'Cooldown between OTP resends in seconds', 'INTEGER'),
    ('otp.max.requests.per.hour', '5', 'OTP', 'Maximum OTP requests per hour per phone', 'INTEGER'),
    ('loan.min.amount', '1000', 'LOAN', 'Minimum loan amount', 'DECIMAL'),
    ('loan.max.amount', '500000', 'LOAN', 'Maximum loan amount', 'DECIMAL'),
    ('loan.default.percentage', '50', 'LOAN', 'Default loan percentage of last purchase', 'DECIMAL'),
    ('loan.max.outstanding', '1', 'LOAN', 'Maximum outstanding loans per customer', 'INTEGER'),
    ('loan.expiry.days', '30', 'LOAN', 'Loan expiry period in days', 'INTEGER'),
    ('loan.service.fee.percentage', '10', 'LOAN', 'Service fee as percentage of principal', 'DECIMAL'),
    ('loan.currency', 'TZS', 'LOAN', 'Default currency', 'STRING'),
    ('provider.connect.timeout.ms', '5000', 'PROVIDER', 'Provider connection timeout', 'INTEGER'),
    ('provider.read.timeout.ms', '30000', 'PROVIDER', 'Provider read timeout', 'INTEGER'),
    ('reconciliation.check.interval.minutes', '15', 'RECONCILIATION', 'Auto-reconciliation interval', 'INTEGER'),
    ('reconciliation.unknown.threshold.minutes', '5', 'RECONCILIATION', 'Threshold before querying unknown txns', 'INTEGER');

-- ============================================================
-- Seed Loan Rule Configurations
-- ============================================================
INSERT INTO loan_rule_configurations (rule_name, rule_type, rule_value, priority, description) VALUES
    ('DEFAULT_ELIGIBILITY_PERCENTAGE', 'ELIGIBILITY', '{"percentage": 50}', 1, 'Eligible amount is 50% of last purchase amount'),
    ('MIN_LAST_PURCHASE_AMOUNT', 'ELIGIBILITY', '{"minAmount": 5000}', 2, 'Minimum last purchase amount to qualify'),
    ('MAX_DAYS_SINCE_PURCHASE', 'ELIGIBILITY', '{"maxDays": 90}', 3, 'Maximum days since last electricity purchase'),
    ('OUTSTANDING_LOAN_CHECK', 'ELIGIBILITY', '{"maxOutstanding": 1}', 4, 'Maximum number of outstanding loans'),
    ('DAILY_LOAN_LIMIT', 'LIMIT', '{"maxLoansPerDay": 100, "maxAmountPerDay": 10000000}', 1, 'Daily loan issuance limits'),
    ('MONTHLY_LOAN_LIMIT', 'LIMIT', '{"maxLoansPerMonth": 2000, "maxAmountPerMonth": 200000000}', 2, 'Monthly loan issuance limits');
