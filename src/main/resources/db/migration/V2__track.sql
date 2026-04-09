-- V2__track.sql
-- Track entity with ISRC (ISO 3901) as natural key

CREATE TABLE track (
    id               UUID PRIMARY KEY,
    artist_id        UUID NOT NULL REFERENCES artist(id) ON DELETE CASCADE,
    title            VARCHAR(1000) NOT NULL,
    isrc             VARCHAR(12) NOT NULL UNIQUE,
    genre            VARCHAR(200),
    duration_seconds INTEGER NOT NULL,
    version          BIGINT NOT NULL DEFAULT 0,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_track_artist_id ON track (artist_id);
