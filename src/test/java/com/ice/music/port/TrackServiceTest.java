package com.ice.music.port;

import com.ice.music.domain.model.Artist;
import com.ice.music.domain.model.ArtistNotFoundException;
import com.ice.music.domain.model.AuditEvent;
import com.ice.music.domain.model.EventType;
import com.ice.music.domain.model.Track;
import com.ice.music.domain.service.TrackService;
import com.ice.music.port.out.ActorContext;
import com.ice.music.port.out.ArtistRepository;
import com.ice.music.port.out.AuditPublisher;
import com.ice.music.port.out.TrackRepository;
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
class TrackServiceTest {

    @Mock private TrackRepository trackRepository;
    @Mock private ArtistRepository artistRepository;
    @Mock private AuditPublisher auditPublisher;
    @Mock private ActorContext actorContext;

    @InjectMocks
    private TrackService trackService;

    @Test
    void addTrack_savesWhenArtistExists() {
        var artist = Artist.create("Queen");
        when(artistRepository.findById(artist.id())).thenReturn(Optional.of(artist));
        when(trackRepository.save(any(Track.class))).thenAnswer(inv -> inv.getArgument(0));
        when(actorContext.currentActorId()).thenReturn("user-42");

        var result = trackService.addTrack(artist.id(), "Bohemian Rhapsody", "GBAYE7500101", "Rock", 354);

        assertThat(result.title()).isEqualTo("Bohemian Rhapsody");
    }

    @Test
    void addTrack_emitsAuditEventWithActor() {
        var artist = Artist.create("Queen");
        when(artistRepository.findById(artist.id())).thenReturn(Optional.of(artist));
        when(trackRepository.save(any(Track.class))).thenAnswer(inv -> inv.getArgument(0));
        when(actorContext.currentActorId()).thenReturn("ingester-1");

        trackService.addTrack(artist.id(), "Bohemian Rhapsody", "GBAYE7500101", "Rock", 354);

        var captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPublisher).publish(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo(EventType.TRACK_ADDED);
        assertThat(captor.getValue().actorId()).isEqualTo("ingester-1");
        assertThat(captor.getValue().after()).containsEntry("isrc", "GBAYE7500101");
    }

    @Test
    void addTrack_throwsWhenArtistNotFound_noAudit() {
        var missingId = UUID.randomUUID();
        when(artistRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trackService.addTrack(missingId, "Title", "GBAYE7500101", "Rock", 354))
                .isInstanceOf(ArtistNotFoundException.class);

        verify(auditPublisher, never()).publish(any());
    }

    @Test
    void fetchTracks_returnsTracksWhenArtistExists() {
        var artist = Artist.create("Queen");
        var track = Track.create(artist.id(), "Bohemian Rhapsody", "GBAYE7500101", "Rock", 354);
        when(artistRepository.findById(artist.id())).thenReturn(Optional.of(artist));
        when(trackRepository.findByArtistId(artist.id())).thenReturn(List.of(track));

        assertThat(trackService.fetchTracks(artist.id())).hasSize(1);
    }

    @Test
    void fetchTracks_throwsWhenArtistNotFound() {
        var missingId = UUID.randomUUID();
        when(artistRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trackService.fetchTracks(missingId))
                .isInstanceOf(ArtistNotFoundException.class);
    }

    @Test
    void fetchTracks_returnsEmptyList() {
        var artist = Artist.create("Queen");
        when(artistRepository.findById(artist.id())).thenReturn(Optional.of(artist));
        when(trackRepository.findByArtistId(artist.id())).thenReturn(List.of());

        assertThat(trackService.fetchTracks(artist.id())).isEmpty();
    }
}
