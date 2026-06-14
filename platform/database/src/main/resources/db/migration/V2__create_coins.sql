CREATE TABLE IF NOT EXISTS coin_sets (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_coin_sets_user_id ON coin_sets(user_id);

DROP TRIGGER IF EXISTS trg_coin_sets_updated_at ON coin_sets;
CREATE TRIGGER trg_coin_sets_updated_at
BEFORE UPDATE ON coin_sets
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

CREATE TABLE IF NOT EXISTS catalog_coins (
    id UUID PRIMARY KEY,
    country_or_issuer TEXT,
    denomination TEXT,
    series_name TEXT,
    title TEXT,
    year INT,
    mint_mark TEXT,
    composition TEXT,
    weight_grams DOUBLE PRECISION,
    diameter_mm DOUBLE PRECISION,
    obverse_description TEXT,
    reverse_description TEXT,
    historical_context TEXT,
    thumbnail_url TEXT,
    numista_url TEXT,
    enriched_at TIMESTAMP WITH TIME ZONE,
    last_enrichment_attempt_at TIMESTAMP WITH TIME ZONE,
    last_enrichment_failed_at TIMESTAMP WITH TIME ZONE,
    last_enrichment_error TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_catalog_coins_fingerprint
    ON catalog_coins(
        COALESCE(country_or_issuer, '__null__'),
        COALESCE(denomination, '__null__'),
        COALESCE(series_name, '__null__'),
        COALESCE(year, -999999),
        COALESCE(mint_mark, '__null__')
    );

DROP TRIGGER IF EXISTS trg_catalog_coins_updated_at ON catalog_coins;
CREATE TRIGGER trg_catalog_coins_updated_at
BEFORE UPDATE ON catalog_coins
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

CREATE TABLE IF NOT EXISTS coins (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    obverse_key TEXT NOT NULL,
    reverse_key TEXT NOT NULL,
    set_id UUID REFERENCES coin_sets(id) ON DELETE SET NULL,
    catalog_coin_id UUID REFERENCES catalog_coins(id) ON DELETE SET NULL,
    notes TEXT,
    overall_confidence VARCHAR(16) NOT NULL,
    country_or_issuer TEXT,
    denomination TEXT,
    series_name TEXT,
    year INT,
    era TEXT,
    mint_mark TEXT,
    metal_composition TEXT,
    estimated_grade TEXT,
    estimated_grade_value TEXT,
    rarity_qualitative TEXT,
    value_low DOUBLE PRECISION,
    value_high DOUBLE PRECISION,
    mintage BIGINT,
    obverse_description TEXT,
    reverse_description TEXT,
    historical_context TEXT,
    -- Specifications
    weight_grams DOUBLE PRECISION,
    diameter_mm DOUBLE PRECISION,
    thickness_mm DOUBLE PRECISION,
    edge TEXT,
    designer_obverse TEXT,
    designer_reverse TEXT,
    -- Condition
    positive_features TEXT[],
    negative_features TEXT[],
    -- Market
    supply_summary TEXT,
    demand_summary TEXT,
    -- Design lettering
    obverse_lettering TEXT,
    reverse_lettering TEXT,
    analysis_notes TEXT,
    raw_json TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_coins_user_id ON coins(user_id);
CREATE INDEX IF NOT EXISTS idx_coins_user_id_country ON coins(user_id, country_or_issuer);
CREATE INDEX IF NOT EXISTS idx_coins_user_id_year ON coins(user_id, year);
CREATE INDEX IF NOT EXISTS idx_coins_set_id ON coins(set_id);
CREATE INDEX IF NOT EXISTS idx_coins_catalog_coin_id ON coins(catalog_coin_id);
CREATE INDEX IF NOT EXISTS idx_coins_created_at ON coins(created_at);

DROP TRIGGER IF EXISTS trg_coins_updated_at ON coins;
CREATE TRIGGER trg_coins_updated_at
BEFORE UPDATE ON coins
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

CREATE TABLE IF NOT EXISTS coin_catalogue_numbers (
    id UUID PRIMARY KEY,
    coin_id UUID NOT NULL REFERENCES coins(id) ON DELETE CASCADE,
    catalogue_name TEXT NOT NULL,
    number TEXT,
    confidence VARCHAR(16) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_coin_catalogue_numbers_coin_id ON coin_catalogue_numbers(coin_id);

CREATE TABLE IF NOT EXISTS external_coin_references (
    id UUID PRIMARY KEY,
    catalog_coin_id UUID NOT NULL REFERENCES catalog_coins(id) ON DELETE CASCADE,
    provider VARCHAR(64) NOT NULL,
    external_id TEXT NOT NULL,
    external_url TEXT,
    last_synced_at TIMESTAMP WITH TIME ZONE,
    sync_status VARCHAR(32),
    sync_error TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_external_coin_refs_provider_ext_id
    ON external_coin_references(provider, external_id);
CREATE UNIQUE INDEX IF NOT EXISTS ux_external_coin_refs_catalog_provider
    ON external_coin_references(catalog_coin_id, provider);
