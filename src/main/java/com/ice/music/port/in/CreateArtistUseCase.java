package com.ice.music.port.in;

import com.ice.music.domain.model.Artist;

/**
 * Inbound port: create a new artist in the catalogue.
 */
public interface CreateArtistUseCase {

    Artist create(String name);
}
