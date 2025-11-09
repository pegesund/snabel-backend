-- Create API clients table for OAuth2 client credentials flow
CREATE TABLE api_clients (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    client_id VARCHAR(50) UNIQUE NOT NULL,
    client_secret_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    scopes VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT true,
    created_by BIGINT REFERENCES users(id)
);

-- Indexes
CREATE INDEX idx_api_clients_customer ON api_clients(customer_id);
CREATE INDEX idx_api_clients_client_id ON api_clients(client_id);
CREATE INDEX idx_api_clients_active ON api_clients(active);

-- Comments
COMMENT ON TABLE api_clients IS 'API clients for OAuth2 client credentials authentication';
COMMENT ON COLUMN api_clients.client_id IS 'Unique client identifier (e.g., client_abc123xyz)';
COMMENT ON COLUMN api_clients.client_secret_hash IS 'BCrypt hash of client secret';
COMMENT ON COLUMN api_clients.scopes IS 'Comma-separated permissions (e.g., read:accounts,write:invoices)';
