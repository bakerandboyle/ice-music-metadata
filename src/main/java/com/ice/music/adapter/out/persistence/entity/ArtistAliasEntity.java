package com.ice.music.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "artist_alias")
@EntityListeners(AuditingEntityListener.class)
public class ArtistAliasEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "artist_id", nullable = false, updatable = false)
    private UUID artistId;

    @Column(name = "alias_name", nullable = false, length = 500)
    private String aliasName;

    @Version
    private Long version;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ArtistAliasEntity() {
    }

    public static ArtistAliasEntity forInsert(UUID id, UUID artistId, String aliasName) {
        var entity = new ArtistAliasEntity();
        entity.id = id;
        entity.artistId = artistId;
        entity.aliasName = aliasName;
        return entity;
    }

    public UUID getId() { return id; }
    public UUID getArtistId() { return artistId; }
    public String getAliasName() { return aliasName; }
    public Instant getCreatedAt() { return createdAt; }
}
