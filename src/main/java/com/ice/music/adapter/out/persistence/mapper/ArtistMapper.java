package com.ice.music.adapter.out.persistence.mapper;

import com.ice.music.adapter.out.persistence.entity.ArtistEntity;
import com.ice.music.domain.model.Artist;
import org.springframework.stereotype.Component;

/**
 * Maps between the domain Artist record and the JPA ArtistEntity.
 *
 * Manual mapping by design - no MapStruct, no reflection magic.
 * Updates bypass the mapper entirely: the repository mutates
 * the managed entity in-place for Hibernate dirty-checking.
 */
@Component
public class ArtistMapper {

    /**
     * Maps a domain Artist to a new JPA entity for INSERT.
     * Version is null so Spring Data calls persist() not merge().
     * Timestamps are null - @CreatedDate/@LastModifiedDate handle them.
     */
    public ArtistEntity toNewEntity(Artist artist) {
        return new ArtistEntity(
                artist.id(),
                artist.name(),
                null,
                null,
                null
        );
    }

    public Artist toDomain(ArtistEntity entity) {
        return new Artist(
                entity.getId(),
                entity.getName(),
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
