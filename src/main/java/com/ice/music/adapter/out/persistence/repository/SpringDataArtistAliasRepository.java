package com.ice.music.adapter.out.persistence.repository;

import com.ice.music.adapter.out.persistence.entity.ArtistAliasEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface SpringDataArtistAliasRepository extends JpaRepository<ArtistAliasEntity, UUID> {

    @Query("SELECT a.artistId FROM ArtistAliasEntity a WHERE LOWER(a.aliasName) = LOWER(:aliasName)")
    List<UUID> findArtistIdsByAliasNameIgnoreCase(String aliasName);

}
