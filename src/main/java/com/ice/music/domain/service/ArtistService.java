package com.ice.music.domain.service;

import com.ice.music.domain.model.Artist;
import com.ice.music.domain.model.ArtistNotFoundException;
import com.ice.music.port.in.CreateArtistUseCase;
import com.ice.music.port.in.EditArtistNameUseCase;
import com.ice.music.port.in.FindArtistUseCase;
import com.ice.music.port.out.ArtistRepository;
import com.ice.music.port.out.CachePort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Domain service implementing Artist use cases.
 *
 * Maintains a Redis counter (artist:count) on create to avoid
 * COUNT(*) table scans in the AOD engine.
 */
@Service
@Transactional
public class ArtistService implements CreateArtistUseCase, EditArtistNameUseCase, FindArtistUseCase {

    static final String ARTIST_COUNT_KEY = "artist:count";

    private final ArtistRepository artistRepository;
    private final CachePort cache;

    public ArtistService(ArtistRepository artistRepository, CachePort cache) {
        this.artistRepository = artistRepository;
        this.cache = cache;
    }

    @Override
    public Artist create(String name) {
        var artist = artistRepository.save(Artist.create(name));
        cache.increment(ARTIST_COUNT_KEY);
        return artist;
    }

    @Override
    public Artist editName(UUID artistId, String newName) {
        return artistRepository.findById(artistId)
                .map(artist -> artist.withName(newName))
                .map(artistRepository::save)
                .orElseThrow(() -> new ArtistNotFoundException(artistId));
    }

    @Override
    @Transactional(readOnly = true)
    public Artist findById(UUID id) {
        return artistRepository.findById(id)
                .orElseThrow(() -> new ArtistNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Artist> findByName(String name) {
        return artistRepository.findByNameIgnoreCase(name);
    }
}
