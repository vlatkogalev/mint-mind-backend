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
    raw_json TEXT NOT NULL
);

CREATE TABLE coin_catalogue_numbers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    coin_id UUID NOT NULL REFERENCES coins(id) ON DELETE CASCADE,
    catalogue_name TEXT NOT NULL,
    number TEXT,
    confidence TEXT NOT NULL
);

CREATE INDEX idx_coins_user_id ON coins(user_id);
CREATE INDEX idx_coins_country ON coins(user_id, country_or_issuer);
CREATE INDEX idx_coins_year ON coins(user_id, year);
CREATE INDEX idx_catalogue_numbers_coin_id ON coin_catalogue_numbers(coin_id);
