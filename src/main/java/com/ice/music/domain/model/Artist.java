package com.ice.music.domain.model;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Core domain representation of an Artist.
 *
 * Pure Java record - no Spring, no JPA, no framework annotations.
 * This is The Sanctuary: business truth lives here.
 */
public record Artist(
        UUID id,
        String name,
        long version,
        Instant createdAt,
        Instant updatedAt
) {
    /**
     * Factory for creating a new Artist with generated ID and timestamps.
     */
    public static Artist create(String name) {
        String normalizedName = Optional.ofNullable(name)
                .map(String::strip)
                .filter(n -> !n.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("Artist name must not be blank"));

        Instant now = Instant.now();
        return new Artist(UUID.randomUUID(), normalizedName, 0L, now, now);
    }

    /**
     * Returns a new Artist with the name changed.
     * Immutable - does not mutate the original.
     */
    public Artist withName(String newName) {
        String normalizedName = Optional.ofNullable(newName)
                .map(String::strip)
                .filter(n -> !n.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("Artist name must not be blank"));
        return new Artist(this.id, normalizedName, this.version, this.createdAt, this.updatedAt);
    }
}
