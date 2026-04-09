package com.ice.music.port.out;

import com.ice.music.domain.model.Track;

import java.util.List;
import java.util.UUID;

/**
 * Outbound port for Track persistence.
 */
public interface TrackRepository {

    Track save(Track track);

    List<Track> findByArtistId(UUID artistId);
}
