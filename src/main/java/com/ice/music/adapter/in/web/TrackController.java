package com.ice.music.adapter.in.web;

import com.ice.music.adapter.in.web.dto.CreateTrackRequest;
import com.ice.music.adapter.in.web.dto.TrackResponse;
import com.ice.music.port.in.AddTrackUseCase;
import com.ice.music.port.in.FetchArtistTracksUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
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
@RequestMapping(path = "/api/artists/{artistId}/tracks", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Tracks", description = "Track management — add and list recordings under an artist")
public class TrackController {

    private final AddTrackUseCase addTrackUseCase;
    private final FetchArtistTracksUseCase fetchArtistTracksUseCase;

    public TrackController(AddTrackUseCase addTrackUseCase, FetchArtistTracksUseCase fetchArtistTracksUseCase) {
        this.addTrackUseCase = addTrackUseCase;
        this.fetchArtistTracksUseCase = fetchArtistTracksUseCase;
    }

    @PostMapping
    @Idempotent(namespace = "track:create")
    @Operation(summary = "Add a track to an artist",
            description = "Registers a recording with an ISRC (ISO 3901) natural key. "
                    + "Duplicate ISRCs are rejected. Supports X-Idempotency-Key.")
    @ApiResponse(responseCode = "201", description = "Track created",
            content = @Content(schema = @Schema(implementation = TrackResponse.class)))
    @ApiResponse(responseCode = "400", description = "Validation failure (missing title, invalid ISRC length, non-positive duration)",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Artist not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Duplicate ISRC or idempotency conflict",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
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
    @Operation(summary = "List all tracks for an artist")
    @ApiResponse(responseCode = "200", description = "Track list (may be empty)")
    @ApiResponse(responseCode = "404", description = "Artist not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public List<TrackResponse> fetchTracks(@PathVariable UUID artistId) {
        return fetchArtistTracksUseCase.fetchTracks(artistId).stream()
                .map(TrackResponse::from)
                .toList();
    }
}
