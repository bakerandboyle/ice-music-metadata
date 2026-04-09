package com.ice.music.adapter.out.persistence;

import com.ice.music.adapter.out.persistence.entity.ArtistEntity;
import com.ice.music.adapter.out.persistence.mapper.ArtistMapper;
import com.ice.music.domain.model.Artist;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for ArtistMapper - no Spring context needed.
 */
class ArtistMapperTest {

    private final ArtistMapper mapper = new ArtistMapper();

    @Test
    void toNewEntity_mapsIdAndName() {
        Artist domain = Artist.create("Queen");
        ArtistEntity entity = mapper.toNewEntity(domain);

        assertThat(entity.getId()).isEqualTo(domain.id());
        assertThat(entity.getName()).isEqualTo(domain.name());
    }

    @Test
    void toNewEntity_setsVersionNull_forPersistDetection() {
        Artist domain = Artist.create("Queen");
        ArtistEntity entity = mapper.toNewEntity(domain);

        assertThat(entity.getVersion()).isNull();
    }

    @Test
    void toNewEntity_setsTimestampsNull_forAuditingListener() {
        Artist domain = Artist.create("Queen");
        ArtistEntity entity = mapper.toNewEntity(domain);

        assertThat(entity.getCreatedAt()).isNull();
        assertThat(entity.getUpdatedAt()).isNull();
    }

    @Test
    void toDomain_mapsAllFields() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        ArtistEntity entity = new ArtistEntity(id, "Queen", 3L, now, now);

        Artist domain = mapper.toDomain(entity);

        assertThat(domain.id()).isEqualTo(id);
        assertThat(domain.name()).isEqualTo("Queen");
        assertThat(domain.version()).isEqualTo(3L);
        assertThat(domain.createdAt()).isEqualTo(now);
        assertThat(domain.updatedAt()).isEqualTo(now);
    }
}
