package com.ice.music.adapter.in.web;

import com.ice.music.adapter.in.web.dto.CreateTrackRequest;
import com.ice.music.adapter.in.web.dto.TrackResponse;
import com.ice.music.port.in.AddTrackUseCase;
import com.ice.music.port.in.FetchArtistTracksUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * REST adapter for Track operations.
 */
@RestController
@RequestMapping("/api/artists/{artistId}/tracks")
public class TrackController {

    private final AddTrackUseCase addTrackUseCase;
    private final FetchArtistTracksUseCase fetchArtistTracksUseCase;

    public TrackController(AddTrackUseCase addTrackUseCase, FetchArtistTracksUseCase fetchArtistTracksUseCase) {
        this.addTrackUseCase = addTrackUseCase;
        this.fetchArtistTracksUseCase = fetchArtistTracksUseCase;
    }

    @PostMapping
    public ResponseEntity<TrackResponse> addTrack(
            @PathVariable UUID artistId,
            @Valid @RequestBody CreateTrackRequest request
    ) {
        var track = addTrackUseCase.addTrack(
                artistId, request.title(), request.isrc(), request.genre(), request.durationSeconds());
        var response = TrackResponse.from(track);
        return ResponseEntity
                .created(URI.create("/api/artists/" + artistId + "/tracks/" + track.id()))
                .body(response);
    }

    @GetMapping
    public List<TrackResponse> fetchTracks(@PathVariable UUID artistId) {
        return fetchArtistTracksUseCase.fetchTracks(artistId).stream()
                .map(TrackResponse::from)
                .toList();
    }
}
