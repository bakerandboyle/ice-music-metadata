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

/**
 * JPA entity for the track table.
 */
@Entity
@Table(name = "track")
@EntityListeners(AuditingEntityListener.class)
public class TrackEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "artist_id", nullable = false, updatable = false)
    private UUID artistId;

    @Column(nullable = false, length = 1000)
    private String title;

    @Column(nullable = false, length = 12, unique = true)
    private String isrc;

    @Column(length = 200)
    private String genre;

    @Column(name = "duration_seconds", nullable = false)
    private Integer durationSeconds;

    @Version
    private Long version;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TrackEntity() {
    }

    /**
     * Factory for creating an entity ready for INSERT.
     * Version and timestamps null — JPA infrastructure manages them.
     */
    public static TrackEntity forInsert(UUID id, UUID artistId, String title,
                                        String isrc, String genre, int durationSeconds) {
        var entity = new TrackEntity();
        entity.id = id;
        entity.artistId = artistId;
        entity.title = title;
        entity.isrc = isrc;
        entity.genre = genre;
        entity.durationSeconds = durationSeconds;
        return entity;
    }

    public UUID getId() { return id; }
    public UUID getArtistId() { return artistId; }
    public String getTitle() { return title; }
    public String getIsrc() { return isrc; }
    public String getGenre() { return genre; }
    public Integer getDurationSeconds() { return durationSeconds; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
