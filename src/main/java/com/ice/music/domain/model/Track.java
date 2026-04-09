package com.ice.music.domain.model;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Core domain representation of a Track.
 *
 * Pure Java record — no Spring, no JPA, no framework annotations.
 * ISRC (International Standard Recording Code, ISO 3901) is the
 * industry-standard unique identifier for a recording.
 */
public record Track(
        UUID id,
        UUID artistId,
        String title,
        String isrc,
        String genre,
        int durationSeconds,
        Instant createdAt,
        Instant updatedAt
) {
    /**
     * Factory for creating a new Track with generated ID and timestamps.
     */
    public static Track create(UUID artistId, String title, String isrc, String genre, int durationSeconds) {
        var validArtistId = Optional.ofNullable(artistId)
                .orElseThrow(() -> new IllegalArgumentException("Artist ID must not be null"));

        var validTitle = Optional.ofNullable(title)
                .map(String::strip)
                .filter(t -> !t.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("Track title must not be blank"));

        var validIsrc = Optional.ofNullable(isrc)
                .map(String::strip)
                .filter(i -> i.length() == 12)
                .orElseThrow(() -> new IllegalArgumentException("ISRC must be exactly 12 characters"));

        if (durationSeconds <= 0) {
            throw new IllegalArgumentException("Duration must be positive");
        }

        var now = Instant.now();
        return new Track(UUID.randomUUID(), validArtistId, validTitle, validIsrc, genre, durationSeconds, now, now);
    }
}
