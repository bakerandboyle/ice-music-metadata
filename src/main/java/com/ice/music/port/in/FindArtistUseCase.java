package com.ice.music.port.in;

import com.ice.music.domain.model.Artist;

import java.util.List;
import java.util.UUID;

/**
 * Inbound port: look up artists by ID or name.
 */
public interface FindArtistUseCase {

    Artist findById(UUID id);

    List<Artist> findByName(String name);
}
