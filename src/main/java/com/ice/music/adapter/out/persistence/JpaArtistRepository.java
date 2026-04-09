package com.ice.music.adapter.out.persistence;

import com.ice.music.adapter.out.persistence.mapper.ArtistMapper;
import com.ice.music.adapter.out.persistence.repository.SpringDataArtistRepository;
import com.ice.music.domain.model.Artist;
import com.ice.music.port.out.ArtistRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA adapter implementing the domain ArtistRepository port.
 *
 * Insert: toNewEntity() with null @Version - Spring Data calls persist().
 * Update: find managed entity, mutate in place - Hibernate dirty-checks
 *         and increments @Version automatically. No detached merge.
 */
@Repository
public class JpaArtistRepository implements ArtistRepository {

    private final SpringDataArtistRepository springDataRepo;
    private final ArtistMapper mapper;

    public JpaArtistRepository(SpringDataArtistRepository springDataRepo, ArtistMapper mapper) {
        this.springDataRepo = springDataRepo;
        this.mapper = mapper;
    }

    @Override
    public Artist save(Artist artist) {
        var entity = springDataRepo.findById(artist.id())
                .map(managed -> {
                    managed.setName(artist.name());
                    return managed;
                })
                .orElseGet(() -> mapper.toNewEntity(artist));
        var saved = springDataRepo.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Artist> findById(UUID id) {
        return springDataRepo.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Artist> findByNameIgnoreCase(String name) {
        return springDataRepo.findByNameIgnoreCase(name).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long count() {
        return springDataRepo.count();
    }
}
