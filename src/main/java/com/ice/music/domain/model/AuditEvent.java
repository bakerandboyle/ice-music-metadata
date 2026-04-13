package com.ice.music.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Typed audit event for the compliance trail.
 *
 * Factory methods centralise the event schema — no magic strings
 * in service code. actorId identifies who enacted the change.
 */
public record AuditEvent(
        EventType eventType,
        UUID entityId,
        EntityType entityType,
        String actorId,
        Map<String, Object> before,
        Map<String, Object> after,
        Instant timestamp
) {

    // --- Artist events ---

    public static AuditEvent artistCreated(Artist artist, String actorId) {
        return new AuditEvent(
                EventType.ARTIST_CREATED, artist.id(), EntityType.ARTIST, actorId,
                Map.of(),
                Map.of("name", artist.name()),
                Instant.now());
    }

    public static AuditEvent artistNameUpdated(UUID artistId, String beforeName, String afterName, String actorId) {
        return new AuditEvent(
                EventType.ARTIST_NAME_UPDATED, artistId, EntityType.ARTIST, actorId,
                Map.of("name", beforeName),
                Map.of("name", afterName),
                Instant.now());
    }

    public static AuditEvent artistAliasAdded(UUID artistId, ArtistAlias alias, String actorId) {
        return new AuditEvent(
                EventType.ARTIST_ALIAS_ADDED, artistId, EntityType.ALIAS, actorId,
                Map.of(),
                Map.of("aliasName", alias.aliasName()),
                Instant.now());
    }

    // --- Track events ---

    public static AuditEvent trackAdded(Track track, String actorId) {
        return new AuditEvent(
                EventType.TRACK_ADDED, track.id(), EntityType.TRACK, actorId,
                Map.of(),
                Map.of("title", track.title(),
                       "isrc", track.isrc(),
                       "artistId", track.artistId().toString()),
                Instant.now());
    }
}
