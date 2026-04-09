package com.ice.music.adapter.out.persistence.mapper;

import com.ice.music.adapter.out.persistence.entity.TrackEntity;
import com.ice.music.domain.model.Track;
import org.springframework.stereotype.Component;

/**
 * Maps between the domain Track record and the JPA TrackEntity.
 */
@Component
public class TrackMapper {

    public TrackEntity toNewEntity(Track track) {
        return TrackEntity.forInsert(
                track.id(),
                track.artistId(),
                track.title(),
                track.isrc(),
                track.genre(),
                track.durationSeconds()
        );
    }

    public Track toDomain(TrackEntity entity) {
        return new Track(
                entity.getId(),
                entity.getArtistId(),
                entity.getTitle(),
                entity.getIsrc(),
                entity.getGenre(),
                entity.getDurationSeconds(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
