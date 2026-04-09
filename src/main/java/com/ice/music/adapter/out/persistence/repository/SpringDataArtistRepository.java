package com.ice.music.adapter.out.persistence.repository;

import com.ice.music.adapter.out.persistence.entity.ArtistEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for ArtistEntity.
 *
 * This is an infrastructure concern - the domain port (ArtistRepository)
 * is the contract. This interface is an implementation detail.
 */
public interface SpringDataArtistRepository extends JpaRepository<ArtistEntity, UUID> {

    List<ArtistEntity> findByNameIgnoreCase(String name);
}
