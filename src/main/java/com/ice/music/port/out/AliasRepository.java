package com.ice.music.port.out;

import com.ice.music.domain.model.ArtistAlias;

import java.util.List;
import java.util.UUID;

/**
 * Outbound port for Artist Alias persistence.
 */
public interface AliasRepository {

    ArtistAlias save(ArtistAlias alias);

    /**
     * Find all artist IDs that have an alias matching the given name (case-insensitive).
     */
    List<UUID> findArtistIdsByAliasIgnoreCase(String aliasName);
}
