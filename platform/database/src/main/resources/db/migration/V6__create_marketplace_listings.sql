CREATE TABLE marketplace_listings (
    id UUID PRIMARY KEY,
    ebay_item_id TEXT NOT NULL UNIQUE,
    title TEXT NOT NULL,
    price TEXT NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    condition TEXT,
    listing_url TEXT NOT NULL,
    image_url TEXT,
    buying_options TEXT[] NOT NULL DEFAULT '{}',
    expires_at TIMESTAMPTZ,
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_marketplace_listings_last_seen_at
    ON marketplace_listings(last_seen_at DESC);
