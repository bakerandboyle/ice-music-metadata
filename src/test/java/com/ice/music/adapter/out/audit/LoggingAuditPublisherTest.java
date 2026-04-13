package com.ice.music.adapter.out.audit;

import com.ice.music.domain.model.Artist;
import com.ice.music.domain.model.AuditEvent;
import com.ice.music.domain.model.Track;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;

class LoggingAuditPublisherTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final LoggingAuditPublisher publisher = new LoggingAuditPublisher(jsonMapper);

    @Test
    void publish_serializesArtistCreatedEvent() {
        var artist = Artist.create("Queen");
        var event = AuditEvent.artistCreated(artist, "user-42");
        assertThatNoException().isThrownBy(() -> publisher.publish(event));
    }

    @Test
    void publish_serializesNameUpdateWithBeforeAndAfter() {
        var event = AuditEvent.artistNameUpdated(UUID.randomUUID(), "Queen", "Freddie Mercury", "user-42");
        assertThatNoException().isThrownBy(() -> publisher.publish(event));
    }

    @Test
    void publish_serializesTrackAddedEvent() {
        var track = Track.create(UUID.randomUUID(), "Bohemian Rhapsody", "GBAYE7500101", "Rock", 354);
        var event = AuditEvent.trackAdded(track, "user-42");
        assertThatNoException().isThrownBy(() -> publisher.publish(event));
    }
}
