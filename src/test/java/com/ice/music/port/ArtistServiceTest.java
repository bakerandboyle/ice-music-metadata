package com.ice.music.port;

import com.ice.music.domain.model.Artist;
import com.ice.music.domain.model.ArtistAlias;
import com.ice.music.domain.model.ArtistNotFoundException;
import com.ice.music.domain.service.ArtistService;
import com.ice.music.port.out.AliasRepository;
import com.ice.music.port.out.ArtistRepository;
import com.ice.music.port.out.CachePort;
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

@ExtendWith(MockitoExtension.class)
class ArtistServiceTest {

    @Mock
    private ArtistRepository artistRepository;

    @Mock
    private AliasRepository aliasRepository;

    @Mock
    private CachePort cache;

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
        verify(cache).increment("artist:count");
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

        assertThat(artistService.findById(existing.id()).name()).isEqualTo("Queen");
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
    void findByName_searchesCanonicalNameAndAliases() {
        var artist = Artist.create("Queen");
        when(artistRepository.findByNameIgnoreCase("Freddie")).thenReturn(List.of());
        when(aliasRepository.findArtistIdsByAliasIgnoreCase("Freddie"))
                .thenReturn(List.of(artist.id()));
        when(artistRepository.findById(artist.id())).thenReturn(Optional.of(artist));

        var results = artistService.findByName("Freddie");

        assertThat(results).hasSize(1)
                .first()
                .satisfies(a -> assertThat(a.name()).isEqualTo("Queen"));
    }

    @Test
    void findByName_deduplicatesCanonicalAndAliasMatches() {
        var artist = Artist.create("Queen");
        when(artistRepository.findByNameIgnoreCase("Queen")).thenReturn(List.of(artist));
        when(aliasRepository.findArtistIdsByAliasIgnoreCase("Queen"))
                .thenReturn(List.of(artist.id()));
        when(artistRepository.findById(artist.id())).thenReturn(Optional.of(artist));

        var results = artistService.findByName("Queen");

        assertThat(results).hasSize(1);
    }

    @Test
    void findByName_returnsEmptyWhenNoneMatch() {
        when(artistRepository.findByNameIgnoreCase("Nobody")).thenReturn(List.of());
        when(aliasRepository.findArtistIdsByAliasIgnoreCase("Nobody")).thenReturn(List.of());

        assertThat(artistService.findByName("Nobody")).isEmpty();
    }

    @Test
    void addAlias_savesAliasWhenArtistExists() {
        var artist = Artist.create("Queen");
        when(artistRepository.findById(artist.id())).thenReturn(Optional.of(artist));
        when(aliasRepository.save(any(ArtistAlias.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        var result = artistService.addAlias(artist.id(), "Farrokh Bulsara");

        assertThat(result.aliasName()).isEqualTo("Farrokh Bulsara");
        assertThat(result.artistId()).isEqualTo(artist.id());
        verify(aliasRepository).save(any(ArtistAlias.class));
    }

    @Test
    void addAlias_throwsWhenArtistNotFound() {
        var missingId = UUID.randomUUID();
        when(artistRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> artistService.addAlias(missingId, "Alias"))
                .isInstanceOf(ArtistNotFoundException.class);
    }
}
