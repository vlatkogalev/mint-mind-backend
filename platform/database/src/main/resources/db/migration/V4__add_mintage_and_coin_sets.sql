ALTER TABLE coins ADD COLUMN mintage BIGINT;

CREATE TABLE coin_sets (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_coin_sets_user_id ON coin_sets(user_id);

ALTER TABLE coins ADD COLUMN set_id UUID REFERENCES coin_sets(id) ON DELETE SET NULL;

CREATE INDEX idx_coins_set_id ON coins(set_id);
