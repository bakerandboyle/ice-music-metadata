package com.ice.music.domain.service;

import com.ice.music.domain.model.ArtistNotFoundException;
import com.ice.music.domain.model.AuditEvent;
import com.ice.music.domain.model.Track;
import com.ice.music.port.in.AddTrackUseCase;
import com.ice.music.port.in.FetchArtistTracksUseCase;
import com.ice.music.port.out.ActorContext;
import com.ice.music.port.out.ArtistRepository;
import com.ice.music.port.out.AuditPublisher;
import com.ice.music.port.out.TrackRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TrackService implements AddTrackUseCase, FetchArtistTracksUseCase {

    private final TrackRepository trackRepository;
    private final ArtistRepository artistRepository;
    private final AuditPublisher auditPublisher;
    private final ActorContext actorContext;

    public TrackService(TrackRepository trackRepository, ArtistRepository artistRepository,
                        AuditPublisher auditPublisher, ActorContext actorContext) {
        this.trackRepository = trackRepository;
        this.artistRepository = artistRepository;
        this.auditPublisher = auditPublisher;
        this.actorContext = actorContext;
    }

    @Override
    public Track addTrack(UUID artistId, String title, String isrc, String genre, int durationSeconds) {
        artistRepository.findById(artistId)
                .orElseThrow(() -> new ArtistNotFoundException(artistId));

        var track = Track.create(artistId, title, isrc, genre, durationSeconds);
        var saved = trackRepository.save(track);

        auditPublisher.publish(AuditEvent.trackAdded(saved, actorContext.currentActorId()));
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Track> fetchTracks(UUID artistId) {
        artistRepository.findById(artistId)
                .orElseThrow(() -> new ArtistNotFoundException(artistId));

        return trackRepository.findByArtistId(artistId);
    }
}
