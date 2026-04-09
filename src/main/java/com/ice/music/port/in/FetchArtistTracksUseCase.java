package com.ice.music.port.in;

import com.ice.music.domain.model.Track;

import java.util.List;
import java.util.UUID;

/**
 * Inbound port: fetch all tracks for a specific artist.
 */
public interface FetchArtistTracksUseCase {

    List<Track> fetchTracks(UUID artistId);
}
