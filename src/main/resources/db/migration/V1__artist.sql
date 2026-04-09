-- V1__artist.sql
-- Foundation: Artist entity with JPA auditing columns and AOD covering index

CREATE TABLE artist (
    id         UUID PRIMARY KEY,
    name       VARCHAR(500) NOT NULL,
    version    BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- Covering index for Artist of the Day Rank-Pointer query
-- Allows OFFSET to be satisfied from the index without heap access
CREATE INDEX idx_artist_aod_rank ON artist (created_at ASC, id ASC);
