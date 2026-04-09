package com.ice.music.adapter.in.web.dto;

import com.ice.music.domain.model.Track;

import java.time.Instant;
import java.util.UUID;

public record TrackResponse(
        UUID id,
        UUID artistId,
        String title,
        String isrc,
        String genre,
        int durationSeconds,
        Instant createdAt
) {
    public static TrackResponse from(Track track) {
        return new TrackResponse(
                track.id(),
                track.artistId(),
                track.title(),
                track.isrc(),
                track.genre(),
                track.durationSeconds(),
                track.createdAt()
        );
    }
}
