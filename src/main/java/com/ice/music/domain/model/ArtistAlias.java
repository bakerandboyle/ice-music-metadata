package com.ice.music.domain.model;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * An alternative name for an Artist.
 *
 * The canonical name lives on the Artist record; aliases provide
 * O(log n) lookup for any secondary name (stage names, former names, etc.).
 */
public record ArtistAlias(
        UUID id,
        UUID artistId,
        String aliasName,
        Instant createdAt
) {
    public static ArtistAlias create(UUID artistId, String aliasName) {
        var validArtistId = Optional.ofNullable(artistId)
                .orElseThrow(() -> new IllegalArgumentException("Artist ID must not be null"));

        var validAlias = Optional.ofNullable(aliasName)
                .map(String::strip)
                .filter(n -> !n.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("Alias name must not be blank"));

        return new ArtistAlias(UUID.randomUUID(), validArtistId, validAlias, Instant.now());
    }
}
