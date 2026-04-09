package com.ice.music.domain.service;

import com.ice.music.domain.model.Artist;
import com.ice.music.domain.model.ArtistNotFoundException;
import com.ice.music.port.in.CreateArtistUseCase;
import com.ice.music.port.in.EditArtistNameUseCase;
import com.ice.music.port.in.FindArtistUseCase;
import com.ice.music.port.out.ArtistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Domain service implementing Artist use cases.
 *
 * Orchestrates domain logic and delegates persistence to the outbound port.
 */
@Service
@Transactional
public class ArtistService implements CreateArtistUseCase, EditArtistNameUseCase, FindArtistUseCase {

    private final ArtistRepository artistRepository;

    public ArtistService(ArtistRepository artistRepository) {
        this.artistRepository = artistRepository;
    }

    @Override
    public Artist create(String name) {
        return artistRepository.save(Artist.create(name));
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
