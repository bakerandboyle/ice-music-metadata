package com.ice.music.port.out;

import com.ice.music.domain.model.Artist;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for Artist persistence.
 *
 * Defined in the port layer - implemented by infrastructure adapters.
 * The domain never knows whether this is backed by JPA, jOOQ, or a flat file.
 */
public interface ArtistRepository {

    Artist save(Artist artist);

    Optional<Artist> findById(UUID id);

    List<Artist> findByNameIgnoreCase(String name);

    long count();
}
