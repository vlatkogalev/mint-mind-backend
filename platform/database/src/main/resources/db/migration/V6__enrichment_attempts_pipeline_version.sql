ALTER TABLE enrichment_attempts
    ADD COLUMN IF NOT EXISTS pipeline_version INTEGER NOT NULL DEFAULT 1;

CREATE INDEX IF NOT EXISTS idx_enrichment_attempts_hash_version
    ON enrichment_attempts (fingerprint_hash, pipeline_version);
