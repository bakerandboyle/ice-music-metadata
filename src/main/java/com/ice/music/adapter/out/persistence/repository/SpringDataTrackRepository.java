package com.ice.music.adapter.out.persistence.repository;

import com.ice.music.adapter.out.persistence.entity.TrackEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataTrackRepository extends JpaRepository<TrackEntity, UUID> {

    List<TrackEntity> findByArtistId(UUID artistId);
}
