CREATE TABLE catalog_coins (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    country_or_issuer TEXT,
    denomination TEXT,
    series_name TEXT,
    title TEXT,
    year INT,
    mint_mark TEXT,
    enriched_at TIMESTAMPTZ,
    last_enrichment_attempt_at TIMESTAMPTZ,
    last_enrichment_failed_at TIMESTAMPTZ,
    last_enrichment_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE coin_sets (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE coins (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    obverse_key TEXT NOT NULL,
    reverse_key TEXT NOT NULL,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    overall_confidence TEXT NOT NULL,
    country_or_issuer TEXT,
    denomination TEXT,
    series_name TEXT,
    year INT,
    mint_mark TEXT,
    metal_composition TEXT,
    estimated_grade TEXT,
    estimated_grade_value TEXT,
    rarity_qualitative TEXT,
    value_low NUMERIC(15,2),
    value_high NUMERIC(15,2),
    obverse_description TEXT,
    reverse_description TEXT,
    historical_context TEXT,
    raw_json TEXT NOT NULL,
    mintage BIGINT
);

CREATE TABLE coin_catalogue_numbers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    coin_id UUID NOT NULL REFERENCES coins(id) ON DELETE CASCADE,
    catalogue_name TEXT NOT NULL,
    number TEXT,
    confidence TEXT NOT NULL
);

CREATE TABLE external_coin_references (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    catalog_coin_id UUID NOT NULL REFERENCES catalog_coins(id) ON DELETE CASCADE,
    provider VARCHAR(64) NOT NULL,
    external_id TEXT NOT NULL,
    external_url TEXT,
    last_synced_at TIMESTAMPTZ,
    sync_status TEXT,
    sync_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (provider, external_id),
    UNIQUE (catalog_coin_id, provider)
);

ALTER TABLE coins ADD COLUMN set_id UUID REFERENCES coin_sets(id) ON DELETE SET NULL;
ALTER TABLE coins ADD COLUMN catalog_coin_id UUID REFERENCES catalog_coins(id) ON DELETE SET NULL;

CREATE INDEX idx_coins_user_id ON coins(user_id);
CREATE INDEX idx_coins_country ON coins(user_id, country_or_issuer);
CREATE INDEX idx_coins_year ON coins(user_id, year);
CREATE INDEX idx_coins_set_id ON coins(set_id);
CREATE INDEX idx_coins_catalog_coin_id ON coins(catalog_coin_id);
CREATE INDEX idx_catalogue_numbers_coin_id ON coin_catalogue_numbers(coin_id);
CREATE INDEX idx_coin_sets_user_id ON coin_sets(user_id);
CREATE UNIQUE INDEX ux_catalog_coins_fingerprint
    ON catalog_coins(
        COALESCE(country_or_issuer, ''),
        COALESCE(denomination, ''),
        COALESCE(series_name, ''),
        COALESCE(title, ''),
        COALESCE(year, -2147483648),
        COALESCE(mint_mark, '')
    );
CREATE INDEX idx_catalog_coins_lookup
    ON catalog_coins(country_or_issuer, denomination, series_name, title, year);
CREATE INDEX idx_external_coin_references_catalog_coin_id
    ON external_coin_references(catalog_coin_id);
DROP TRIGGER IF EXISTS trg_catalog_coins_updated_at ON catalog_coins;
CREATE TRIGGER trg_catalog_coins_updated_at
    BEFORE UPDATE ON catalog_coins
    FOR EACH ROW
EXECUTE FUNCTION set_updated_at();
