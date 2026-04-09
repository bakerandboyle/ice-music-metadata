package com.ice.music.domain;

import com.ice.music.domain.model.Track;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure domain unit tests for Track — no Spring, no I/O.
 */
class TrackDomainTest {

    private static final UUID ARTIST_ID = UUID.randomUUID();

    @Test
    void create_setsAllFieldsCorrectly() {
        var track = Track.create(ARTIST_ID, "Bohemian Rhapsody", "GBAYE7500101", "Rock", 354);

        assertThat(track.id()).isNotNull();
        assertThat(track.artistId()).isEqualTo(ARTIST_ID);
        assertThat(track.title()).isEqualTo("Bohemian Rhapsody");
        assertThat(track.isrc()).isEqualTo("GBAYE7500101");
        assertThat(track.genre()).isEqualTo("Rock");
        assertThat(track.durationSeconds()).isEqualTo(354);
        assertThat(track.createdAt()).isNotNull();
    }

    @Test
    void create_stripsWhitespaceFromTitle() {
        var track = Track.create(ARTIST_ID, "  Bohemian Rhapsody  ", "GBAYE7500101", "Rock", 354);
        assertThat(track.title()).isEqualTo("Bohemian Rhapsody");
    }

    @Test
    void create_rejectsNullArtistId() {
        assertThatThrownBy(() -> Track.create(null, "Title", "GBAYE7500101", "Rock", 354))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Artist ID");
    }

    @Test
    void create_rejectsBlankTitle() {
        assertThatThrownBy(() -> Track.create(ARTIST_ID, "  ", "GBAYE7500101", "Rock", 354))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("title");
    }

    @Test
    void create_rejectsNullTitle() {
        assertThatThrownBy(() -> Track.create(ARTIST_ID, null, "GBAYE7500101", "Rock", 354))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("title");
    }

    @Test
    void create_rejectsInvalidIsrc() {
        assertThatThrownBy(() -> Track.create(ARTIST_ID, "Title", "SHORT", "Rock", 354))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ISRC");
    }

    @Test
    void create_rejectsNullIsrc() {
        assertThatThrownBy(() -> Track.create(ARTIST_ID, "Title", null, "Rock", 354))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ISRC");
    }

    @Test
    void create_rejectsZeroDuration() {
        assertThatThrownBy(() -> Track.create(ARTIST_ID, "Title", "GBAYE7500101", "Rock", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duration");
    }

    @Test
    void create_rejectsNegativeDuration() {
        assertThatThrownBy(() -> Track.create(ARTIST_ID, "Title", "GBAYE7500101", "Rock", -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duration");
    }

    @Test
    void create_allowsNullGenre() {
        var track = Track.create(ARTIST_ID, "Title", "GBAYE7500101", null, 354);
        assertThat(track.genre()).isNull();
    }
}
