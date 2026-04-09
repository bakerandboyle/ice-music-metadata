package com.ice.music.adapter.out.persistence;

import com.ice.music.adapter.out.persistence.mapper.TrackMapper;
import com.ice.music.adapter.out.persistence.repository.SpringDataTrackRepository;
import com.ice.music.domain.model.Track;
import com.ice.music.port.out.TrackRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * JPA adapter implementing the domain TrackRepository port.
 */
@Repository
public class JpaTrackRepository implements TrackRepository {

    private final SpringDataTrackRepository springDataRepo;
    private final TrackMapper mapper;

    public JpaTrackRepository(SpringDataTrackRepository springDataRepo, TrackMapper mapper) {
        this.springDataRepo = springDataRepo;
        this.mapper = mapper;
    }

    @Override
    public Track save(Track track) {
        var entity = mapper.toNewEntity(track);
        var saved = springDataRepo.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<Track> findByArtistId(UUID artistId) {
        return springDataRepo.findByArtistId(artistId).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
