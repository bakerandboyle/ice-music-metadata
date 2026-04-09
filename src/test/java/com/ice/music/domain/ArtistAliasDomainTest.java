package com.ice.music.domain;

import com.ice.music.domain.model.ArtistAlias;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArtistAliasDomainTest {

    private static final UUID ARTIST_ID = UUID.randomUUID();

    @Test
    void create_setsAllFields() {
        var alias = ArtistAlias.create(ARTIST_ID, "Farrokh Bulsara");

        assertThat(alias.id()).isNotNull();
        assertThat(alias.artistId()).isEqualTo(ARTIST_ID);
        assertThat(alias.aliasName()).isEqualTo("Farrokh Bulsara");
        assertThat(alias.createdAt()).isNotNull();
    }

    @Test
    void create_stripsWhitespace() {
        var alias = ArtistAlias.create(ARTIST_ID, "  Farrokh Bulsara  ");
        assertThat(alias.aliasName()).isEqualTo("Farrokh Bulsara");
    }

    @Test
    void create_rejectsNullArtistId() {
        assertThatThrownBy(() -> ArtistAlias.create(null, "Alias"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Artist ID");
    }

    @Test
    void create_rejectsBlankAlias() {
        assertThatThrownBy(() -> ArtistAlias.create(ARTIST_ID, "  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Alias name");
    }

    @Test
    void create_rejectsNullAlias() {
        assertThatThrownBy(() -> ArtistAlias.create(ARTIST_ID, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Alias name");
    }
}
