package com.ice.music.port.in;

import com.ice.music.domain.model.Artist;

import java.util.Optional;

/**
 * Inbound port: get today's featured artist.
 */
public interface ArtistOfTheDayUseCase {

    Optional<Artist> getArtistOfTheDay();
}
