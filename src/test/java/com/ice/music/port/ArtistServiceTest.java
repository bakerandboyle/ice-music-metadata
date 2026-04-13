package com.ice.music.port;

import com.ice.music.domain.model.Artist;
import com.ice.music.domain.model.ArtistAlias;
import com.ice.music.domain.model.ArtistNotFoundException;
import com.ice.music.domain.model.AuditEvent;
import com.ice.music.domain.model.EventType;
import com.ice.music.domain.service.ArtistService;
import com.ice.music.port.out.ActorContext;
import com.ice.music.port.out.AliasRepository;
import com.ice.music.port.out.ArtistRepository;
import com.ice.music.port.out.AuditPublisher;
import com.ice.music.port.out.CachePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArtistServiceTest {

    @Mock private ArtistRepository artistRepository;
    @Mock private AliasRepository aliasRepository;
    @Mock private CachePort cache;
    @Mock private AuditPublisher auditPublisher;
    @Mock private ActorContext actorContext;

    @InjectMocks
    private ArtistService artistService;

    @Test
    void create_savesAndIncrementsCounter() {
        when(artistRepository.save(any(Artist.class))).thenAnswer(inv -> inv.getArgument(0));
        when(actorContext.currentActorId()).thenReturn("user-42");

        var result = artistService.create("Queen");

        assertThat(result.name()).isEqualTo("Queen");
        verify(cache).increment("artist:count");
    }

    @Test
    void create_emitsAuditEventWithActor() {
        when(artistRepository.save(any(Artist.class))).thenAnswer(inv -> inv.getArgument(0));
        when(actorContext.currentActorId()).thenReturn("user-42");

        var result = artistService.create("Queen");

        var captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPublisher).publish(captor.capture());

        var event = captor.getValue();
        assertThat(event.eventType()).isEqualTo(EventType.ARTIST_CREATED);
        assertThat(event.entityId()).isEqualTo(result.id());
        assertThat(event.actorId()).isEqualTo("user-42");
        assertThat(event.after()).containsEntry("name", "Queen");
    }

    @Test
    void editName_renamesAndSaves() {
        var existing = Artist.create("Queen");
        when(artistRepository.findById(existing.id())).thenReturn(Optional.of(existing));
        when(artistRepository.save(any(Artist.class))).thenAnswer(inv -> inv.getArgument(0));
        when(actorContext.currentActorId()).thenReturn("user-42");

        var result = artistService.editName(existing.id(), "Freddie Mercury");

        assertThat(result.name()).isEqualTo("Freddie Mercury");
    }

    @Test
    void editName_emitsAuditWithBeforeAfterAndActor() {
        var existing = Artist.create("Queen");
        when(artistRepository.findById(existing.id())).thenReturn(Optional.of(existing));
        when(artistRepository.save(any(Artist.class))).thenAnswer(inv -> inv.getArgument(0));
        when(actorContext.currentActorId()).thenReturn("admin-1");

        artistService.editName(existing.id(), "Freddie Mercury");

        var captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPublisher).publish(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo(EventType.ARTIST_NAME_UPDATED);
        assertThat(captor.getValue().actorId()).isEqualTo("admin-1");
        assertThat(captor.getValue().before()).containsEntry("name", "Queen");
        assertThat(captor.getValue().after()).containsEntry("name", "Freddie Mercury");
    }

    @Test
    void editName_throwsWhenNotFound_noAudit() {
        var missingId = UUID.randomUUID();
        when(artistRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> artistService.editName(missingId, "New Name"))
                .isInstanceOf(ArtistNotFoundException.class);

        verify(auditPublisher, never()).publish(any());
    }

    @Test
    void findById_returnsArtist() {
        var existing = Artist.create("Queen");
        when(artistRepository.findById(existing.id())).thenReturn(Optional.of(existing));

        assertThat(artistService.findById(existing.id()).name()).isEqualTo("Queen");
    }

    @Test
    void findById_throwsWhenNotFound() {
        var missingId = UUID.randomUUID();
        when(artistRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> artistService.findById(missingId))
                .isInstanceOf(ArtistNotFoundException.class);
    }

    @Test
    void findByName_searchesCanonicalAndAliases() {
        var artist = Artist.create("Queen");
        when(artistRepository.findByNameIgnoreCase("Freddie")).thenReturn(List.of());
        when(aliasRepository.findArtistIdsByAliasIgnoreCase("Freddie")).thenReturn(List.of(artist.id()));
        when(artistRepository.findById(artist.id())).thenReturn(Optional.of(artist));

        assertThat(artistService.findByName("Freddie")).hasSize(1);
    }

    @Test
    void findByName_deduplicates() {
        var artist = Artist.create("Queen");
        when(artistRepository.findByNameIgnoreCase("Queen")).thenReturn(List.of(artist));
        when(aliasRepository.findArtistIdsByAliasIgnoreCase("Queen")).thenReturn(List.of(artist.id()));
        when(artistRepository.findById(artist.id())).thenReturn(Optional.of(artist));

        assertThat(artistService.findByName("Queen")).hasSize(1);
    }

    @Test
    void findByName_returnsEmptyWhenNoneMatch() {
        when(artistRepository.findByNameIgnoreCase("Nobody")).thenReturn(List.of());
        when(aliasRepository.findArtistIdsByAliasIgnoreCase("Nobody")).thenReturn(List.of());

        assertThat(artistService.findByName("Nobody")).isEmpty();
    }

    @Test
    void addAlias_savesAndEmitsAuditWithActor() {
        var artist = Artist.create("Queen");
        when(artistRepository.findById(artist.id())).thenReturn(Optional.of(artist));
        when(aliasRepository.save(any(ArtistAlias.class))).thenAnswer(inv -> inv.getArgument(0));
        when(actorContext.currentActorId()).thenReturn("curator-5");

        artistService.addAlias(artist.id(), "Farrokh Bulsara");

        var captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPublisher).publish(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo(EventType.ARTIST_ALIAS_ADDED);
        assertThat(captor.getValue().actorId()).isEqualTo("curator-5");
    }

    @Test
    void addAlias_throwsWhenNotFound_noAudit() {
        var missingId = UUID.randomUUID();
        when(artistRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> artistService.addAlias(missingId, "Alias"))
                .isInstanceOf(ArtistNotFoundException.class);

        verify(auditPublisher, never()).publish(any());
    }
}
