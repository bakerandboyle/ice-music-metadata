package com.ice.music.adapter.in.web.dto;

import com.ice.music.domain.model.Artist;

import java.time.Instant;
import java.util.UUID;

public record ArtistResponse(
        UUID id,
        String name,
        Instant createdAt,
        Instant updatedAt
) {
    public static ArtistResponse from(Artist artist) {
        return new ArtistResponse(
                artist.id(),
                artist.name(),
                artist.createdAt(),
                artist.updatedAt()
        );
    }
}
