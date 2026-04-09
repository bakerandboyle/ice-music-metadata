package com.ice.music.adapter.out.persistence;

import com.ice.music.adapter.out.persistence.mapper.ArtistMapper;
import com.ice.music.adapter.out.persistence.repository.SpringDataArtistRepository;
import com.ice.music.domain.model.Artist;
import com.ice.music.port.out.ArtistRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA adapter implementing the domain ArtistRepository port.
 *
 * Insert: toNewEntity() with null @Version — Spring Data calls persist().
 * Update: find managed entity, mutate in place — Hibernate dirty-checks
 *         and increments @Version automatically. No detached merge.
 */
@Repository
public class JpaArtistRepository implements ArtistRepository {

    private static final Sort AOD_SORT = Sort.by(
            Sort.Order.asc("createdAt"),
            Sort.Order.asc("id")
    );

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

    /**
     * Rank-Pointer query: OFFSET into the covering index (created_at, id).
     * Uses Spring Data Pageable — single row at the given offset.
     */
    @Override
    public Optional<Artist> findByRank(long rank) {
        return springDataRepo.findAll(PageRequest.of((int) rank, 1, AOD_SORT))
                .stream()
                .findFirst()
                .map(mapper::toDomain);
    }

    @Override
    public long count() {
        return springDataRepo.count();
    }
}
