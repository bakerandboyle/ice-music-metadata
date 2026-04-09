package com.ice.music.port;

import com.ice.music.domain.model.Artist;
import com.ice.music.domain.model.ArtistNotFoundException;
import com.ice.music.domain.service.ArtistService;
import com.ice.music.port.out.ArtistRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Use case tests with mocked outbound port.
 * Verifies orchestration logic without infrastructure.
 */
@ExtendWith(MockitoExtension.class)
class ArtistServiceTest {

    @Mock
    private ArtistRepository artistRepository;

    @InjectMocks
    private ArtistService artistService;

    @Test
    void create_savesAndReturnsArtist() {
        when(artistRepository.save(any(Artist.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        var result = artistService.create("Queen");

        assertThat(result.name()).isEqualTo("Queen");
        assertThat(result.id()).isNotNull();
        verify(artistRepository).save(any(Artist.class));
    }

    @Test
    void editName_mapsFoundArtistThroughRenameAndSave() {
        var existing = Artist.create("Queen");
        when(artistRepository.findById(existing.id()))
                .thenReturn(Optional.of(existing));
        when(artistRepository.save(any(Artist.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        var result = artistService.editName(existing.id(), "Freddie Mercury");

        assertThat(result.name()).isEqualTo("Freddie Mercury");
        assertThat(result.id()).isEqualTo(existing.id());
        verify(artistRepository).save(any(Artist.class));
    }

    @Test
    void editName_throwsWhenArtistNotFound() {
        var missingId = UUID.randomUUID();
        when(artistRepository.findById(missingId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> artistService.editName(missingId, "New Name"))
                .isInstanceOf(ArtistNotFoundException.class);
    }

    @Test
    void findById_returnsArtist() {
        var existing = Artist.create("Queen");
        when(artistRepository.findById(existing.id()))
                .thenReturn(Optional.of(existing));

        var result = artistService.findById(existing.id());

        assertThat(result.name()).isEqualTo("Queen");
    }

    @Test
    void findById_throwsWhenNotFound() {
        var missingId = UUID.randomUUID();
        when(artistRepository.findById(missingId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> artistService.findById(missingId))
                .isInstanceOf(ArtistNotFoundException.class);
    }

    @Test
    void findByName_delegatesToRepository() {
        var queen = Artist.create("Queen");
        when(artistRepository.findByNameIgnoreCase("Queen"))
                .thenReturn(List.of(queen));

        var results = artistService.findByName("Queen");

        assertThat(results).hasSize(1)
                .first()
                .satisfies(a -> assertThat(a.name()).isEqualTo("Queen"));
    }

    @Test
    void findByName_returnsEmptyListWhenNoneMatch() {
        when(artistRepository.findByNameIgnoreCase("Nobody"))
                .thenReturn(List.of());

        assertThat(artistService.findByName("Nobody")).isEmpty();
    }
}
