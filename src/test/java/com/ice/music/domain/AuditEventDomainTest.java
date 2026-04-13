package com.ice.music.domain;

import com.ice.music.domain.model.*;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuditEventDomainTest {

    private static final String ACTOR = "user-42";

    @Test
    void artistCreated_setsAllFieldsIncludingActor() {
        var artist = Artist.create("Queen");
        var event = AuditEvent.artistCreated(artist, ACTOR);

        assertThat(event.eventType()).isEqualTo(EventType.ARTIST_CREATED);
        assertThat(event.entityId()).isEqualTo(artist.id());
        assertThat(event.entityType()).isEqualTo(EntityType.ARTIST);
        assertThat(event.actorId()).isEqualTo(ACTOR);
        assertThat(event.before()).isEmpty();
        assertThat(event.after()).containsEntry("name", "Queen");
        assertThat(event.timestamp()).isNotNull();
    }

    @Test
    void artistNameUpdated_capturesBeforeAfterAndActor() {
        var id = UUID.randomUUID();
        var event = AuditEvent.artistNameUpdated(id, "Queen", "Freddie Mercury", ACTOR);

        assertThat(event.eventType()).isEqualTo(EventType.ARTIST_NAME_UPDATED);
        assertThat(event.actorId()).isEqualTo(ACTOR);
        assertThat(event.entityType()).isEqualTo(EntityType.ARTIST);
        assertThat(event.before()).containsEntry("name", "Queen");
        assertThat(event.after()).containsEntry("name", "Freddie Mercury");
    }

    @Test
    void artistAliasAdded_capturesAliasAndActor() {
        var artistId = UUID.randomUUID();
        var alias = ArtistAlias.create(artistId, "Farrokh Bulsara");
        var event = AuditEvent.artistAliasAdded(artistId, alias, ACTOR);

        assertThat(event.eventType()).isEqualTo(EventType.ARTIST_ALIAS_ADDED);
        assertThat(event.entityType()).isEqualTo(EntityType.ALIAS);
        assertThat(event.actorId()).isEqualTo(ACTOR);
        assertThat(event.after()).containsEntry("aliasName", "Farrokh Bulsara");
    }

    @Test
    void trackAdded_capturesTrackDetailsAndActor() {
        var track = Track.create(UUID.randomUUID(), "Bohemian Rhapsody", "GBAYE7500101", "Rock", 354);
        var event = AuditEvent.trackAdded(track, ACTOR);

        assertThat(event.eventType()).isEqualTo(EventType.TRACK_ADDED);
        assertThat(event.entityType()).isEqualTo(EntityType.TRACK);
        assertThat(event.actorId()).isEqualTo(ACTOR);
        assertThat(event.after()).containsEntry("title", "Bohemian Rhapsody");
        assertThat(event.after()).containsEntry("isrc", "GBAYE7500101");
    }
}
