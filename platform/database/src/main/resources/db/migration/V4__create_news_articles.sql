CREATE TABLE news_articles
(
    id           UUID PRIMARY KEY,
    guid         TEXT        NOT NULL UNIQUE,
    title        TEXT        NOT NULL,
    link         TEXT        NOT NULL,
    description  TEXT        NOT NULL DEFAULT '',
    content      TEXT        NOT NULL DEFAULT '',
    author       TEXT,
    image_url    TEXT,
    published_at TIMESTAMPTZ NOT NULL,
    fetched_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_news_articles_published_at ON news_articles (published_at DESC);
CREATE INDEX idx_news_articles_guid ON news_articles (guid);
