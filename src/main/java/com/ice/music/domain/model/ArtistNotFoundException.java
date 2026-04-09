package com.ice.music.domain.model;

import java.util.UUID;

/**
 * Thrown when an artist is not found by ID.
 * Domain exception — no framework dependency.
 */
public class ArtistNotFoundException extends RuntimeException {

    private final UUID artistId;

    public ArtistNotFoundException(UUID artistId) {
        super("Artist not found: " + artistId);
        this.artistId = artistId;
    }

    public UUID getArtistId() {
        return artistId;
    }
}
