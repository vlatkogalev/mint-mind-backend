UPDATE catalog_coins SET
    country_or_issuer = LOWER(country_or_issuer),
    denomination = LOWER(denomination);

CREATE TABLE IF NOT EXISTS enrichment_attempts (
    fingerprint_hash TEXT PRIMARY KEY,
    retrieval_key TEXT NOT NULL,
    last_attempt_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_result TEXT NOT NULL CHECK (last_result IN ('MATCHED', 'AMBIGUOUS', 'NO_MATCH'))
);

CREATE INDEX IF NOT EXISTS idx_catalog_coins_lookup
    ON catalog_coins (LOWER(country_or_issuer), LOWER(denomination), year);
