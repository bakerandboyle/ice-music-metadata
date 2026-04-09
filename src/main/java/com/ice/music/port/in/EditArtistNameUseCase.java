package com.ice.music.port.in;

import com.ice.music.domain.model.Artist;

import java.util.UUID;

/**
 * Inbound port: rename an artist's canonical name.
 */
public interface EditArtistNameUseCase {

    Artist editName(UUID artistId, String newName);
}
