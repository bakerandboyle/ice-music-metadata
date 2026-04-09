package com.ice.music.adapter.out.persistence;

import com.ice.music.TestcontainersConfig;
import com.ice.music.adapter.out.persistence.mapper.ArtistMapper;
import com.ice.music.adapter.out.persistence.repository.SpringDataArtistRepository;
import com.ice.music.domain.model.Artist;
import com.ice.music.port.out.ArtistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository tests against real PostgreSQL via Testcontainers.
 *
 * Proves: JPA mapping, Flyway migration, constraints, and audit columns
 * all work against the production database engine.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfig.class, JpaArtistRepository.class, ArtistMapper.class, JpaAuditingConfig.class})
class ArtistRepositoryTest {

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private SpringDataArtistRepository springDataRepo;

    @BeforeEach
    void cleanUp() {
        springDataRepo.deleteAll();
    }

    @Test
    void save_persistsArtistAndReturnsWithId() {
        Artist artist = Artist.create("Queen");
        Artist saved = artistRepository.save(artist);

        assertThat(saved.id()).isEqualTo(artist.id());
        assertThat(saved.name()).isEqualTo("Queen");
    }

    @Test
    void save_populatesAuditTimestamps() {
        Artist artist = Artist.create("Queen");
        Artist saved = artistRepository.save(artist);

        assertThat(saved.createdAt()).isNotNull();
        assertThat(saved.updatedAt()).isNotNull();
    }

    @Test
    void findById_returnsArtistWhenExists() {
        Artist artist = Artist.create("Queen");
        artistRepository.save(artist);

        Optional<Artist> found = artistRepository.findById(artist.id());

        assertThat(found).isPresent();
        assertThat(found.get().name()).isEqualTo("Queen");
    }

    @Test
    void findById_returnsEmptyWhenNotExists() {
        Optional<Artist> found = artistRepository.findById(UUID.randomUUID());
        assertThat(found).isEmpty();
    }

    @Test
    void count_returnsNumberOfArtists() {
        assertThat(artistRepository.count()).isZero();

        artistRepository.save(Artist.create("Queen"));
        artistRepository.save(Artist.create("Radiohead"));

        assertThat(artistRepository.count()).isEqualTo(2);
    }

    @Test
    void save_updatesExistingArtist() {
        Artist artist = Artist.create("Queen");
        Artist saved = artistRepository.save(artist);

        Artist renamed = saved.withName("Freddie Mercury");
        Artist updated = artistRepository.save(renamed);

        assertThat(updated.name()).isEqualTo("Freddie Mercury");
        assertThat(updated.id()).isEqualTo(saved.id());
    }

    @Test
    void optimisticLocking_versionIncrementsOnUpdate() {
        Artist artist = Artist.create("Queen");
        Artist saved = artistRepository.save(artist);
        springDataRepo.flush(); // Force INSERT - version 0 written to DB

        Artist renamed = saved.withName("Freddie Mercury");
        artistRepository.save(renamed);
        springDataRepo.flush(); // Force UPDATE - Hibernate increments version to 1

        // Re-read: managed entity now has version 1 after flush
        Artist updated = artistRepository.findById(artist.id()).orElseThrow();
        assertThat(updated.version()).isGreaterThan(saved.version());
    }
}
