package com.ice.music.port.in;

import com.ice.music.domain.model.Track;

import java.util.UUID;

/**
 * Inbound port: add a new track to an artist's catalogue.
 */
public interface AddTrackUseCase {

    Track addTrack(UUID artistId, String title, String isrc, String genre, int durationSeconds);
}
