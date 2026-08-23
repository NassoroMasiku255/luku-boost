-- API Clients table for Customer API authentication (API Key + Client ID)
CREATE TABLE IF NOT EXISTS api_clients (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_name     VARCHAR(100)  NOT NULL,
    client_id       VARCHAR(64)   NOT NULL UNIQUE,
    api_key_hash    VARCHAR(255)  NOT NULL,
    description     TEXT,
    enabled         BOOLEAN       NOT NULL DEFAULT TRUE,
    allowed_ips     TEXT,
    rate_limit_per_minute INTEGER NOT NULL DEFAULT 60,
    last_used_at    TIMESTAMP WITH TIME ZONE,
    created_by      UUID,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version         BIGINT        NOT NULL DEFAULT 0
);

CREATE INDEX idx_api_clients_client_id ON api_clients(client_id);
