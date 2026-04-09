package com.ice.music.port.in;

import com.ice.music.domain.model.ArtistAlias;

import java.util.UUID;

/**
 * Inbound port: add an alias to an artist.
 */
public interface AddAliasUseCase {

    ArtistAlias addAlias(UUID artistId, String aliasName);
}
