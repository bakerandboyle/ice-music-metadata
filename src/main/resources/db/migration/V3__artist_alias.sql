-- V3__artist_alias.sql
-- Artist alias: secondary names pointing to canonical artist

CREATE TABLE artist_alias (
    id         UUID PRIMARY KEY,
    artist_id  UUID NOT NULL REFERENCES artist(id) ON DELETE CASCADE,
    alias_name VARCHAR(500) NOT NULL,
    version    BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_artist_alias_name ON artist_alias (LOWER(alias_name));
CREATE INDEX idx_artist_alias_artist_id ON artist_alias (artist_id);
