package com.ice.music.domain;

import com.ice.music.domain.model.Artist;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure domain unit tests - no Spring, no I/O.
 * Tests the Artist record's factory and behaviour methods.
 */
class ArtistDomainTest {

    @Test
    void createArtist_setsFieldsCorrectly() {
        Artist artist = Artist.create("Queen");

        assertThat(artist.id()).isNotNull();
        assertThat(artist.name()).isEqualTo("Queen");
        assertThat(artist.version()).isZero();
        assertThat(artist.createdAt()).isNotNull();
        assertThat(artist.updatedAt()).isNotNull();
        assertThat(artist.createdAt()).isEqualTo(artist.updatedAt());
    }

    @Test
    void createArtist_stripsWhitespace() {
        Artist artist = Artist.create("  Queen  ");
        assertThat(artist.name()).isEqualTo("Queen");
    }

    @Test
    void createArtist_rejectsNull() {
        assertThatThrownBy(() -> Artist.create(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be blank");
    }

    @Test
    void createArtist_rejectsBlank() {
        assertThatThrownBy(() -> Artist.create("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be blank");
    }

    @Test
    void withName_returnsNewInstanceWithUpdatedName() {
        Artist original = Artist.create("Queen");
        Artist renamed = original.withName("Freddie Mercury");

        assertThat(renamed.name()).isEqualTo("Freddie Mercury");
        assertThat(renamed.id()).isEqualTo(original.id());
        assertThat(original.name()).isEqualTo("Queen"); // immutable
    }

    @Test
    void withName_stripsWhitespace() {
        Artist artist = Artist.create("Queen");
        Artist renamed = artist.withName("  Freddie Mercury  ");
        assertThat(renamed.name()).isEqualTo("Freddie Mercury");
    }

    @Test
    void withName_rejectsNull() {
        Artist artist = Artist.create("Queen");
        assertThatThrownBy(() -> artist.withName(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be blank");
    }

    @Test
    void withName_rejectsBlank() {
        Artist artist = Artist.create("Queen");
        assertThatThrownBy(() -> artist.withName("  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be blank");
    }

 }
