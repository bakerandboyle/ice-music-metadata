package com.ice.music.domain.model;

/**
 * Strongly typed event schema for the audit trail.
 *
 * Prevents "Audit Drift" — no magic strings, no inconsistent naming.
 * Downstream consumers (OpenSearch dashboards, S3 compliance storage)
 * rely on this as a fixed contract.
 */
public enum EventType {
    ARTIST_CREATED,
    ARTIST_NAME_UPDATED,
    ARTIST_ALIAS_ADDED,
    TRACK_ADDED
}
