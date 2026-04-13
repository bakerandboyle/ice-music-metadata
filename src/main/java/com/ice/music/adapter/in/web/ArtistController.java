package com.ice.music.adapter.in.web;

import com.ice.music.adapter.in.web.dto.AddAliasRequest;
import com.ice.music.adapter.in.web.dto.AliasResponse;
import com.ice.music.adapter.in.web.dto.ArtistResponse;
import com.ice.music.adapter.in.web.dto.CreateArtistRequest;
import com.ice.music.adapter.in.web.dto.EditArtistNameRequest;
import com.ice.music.port.in.AddAliasUseCase;
import com.ice.music.port.in.CreateArtistUseCase;
import com.ice.music.port.in.EditArtistNameUseCase;
import com.ice.music.port.in.FindArtistUseCase;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * REST adapter for Artist operations.
 */
@RestController
@RequestMapping(path = "/api/artists", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Artists", description = "Artist lifecycle — create, rename, search, and alias management")
public class ArtistController {

    private final CreateArtistUseCase createArtistUseCase;
    private final EditArtistNameUseCase editArtistNameUseCase;
    private final FindArtistUseCase findArtistUseCase;
    private final AddAliasUseCase addAliasUseCase;

    public ArtistController(
            CreateArtistUseCase createArtistUseCase,
            EditArtistNameUseCase editArtistNameUseCase,
            FindArtistUseCase findArtistUseCase,
            AddAliasUseCase addAliasUseCase
    ) {
        this.createArtistUseCase = createArtistUseCase;
        this.editArtistNameUseCase = editArtistNameUseCase;
        this.findArtistUseCase = findArtistUseCase;
        this.addAliasUseCase = addAliasUseCase;
    }

    @PostMapping
    @Idempotent(namespace = "artist:create")
    @Operation(summary = "Create a new artist",
            description = "Creates an artist with the given name. Supports X-Idempotency-Key for safe retries.")
    @ApiResponse(responseCode = "201", description = "Artist created",
            content = @Content(schema = @Schema(implementation = ArtistResponse.class)))
    @ApiResponse(responseCode = "400", description = "Validation failure (blank name, exceeds 500 chars)",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Idempotency conflict — duplicate request in flight",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<ArtistResponse> createArtist(@Valid @RequestBody CreateArtistRequest request) {
        var response = ArtistResponse.from(createArtistUseCase.create(request.name()));
        return ResponseEntity
                .created(URI.create("/api/artists/" + response.id()))
                .body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an artist by ID")
    @ApiResponse(responseCode = "200", description = "Artist found",
            content = @Content(schema = @Schema(implementation = ArtistResponse.class)))
    @ApiResponse(responseCode = "404", description = "Artist not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ArtistResponse getArtist(@PathVariable UUID id) {
        return ArtistResponse.from(findArtistUseCase.findById(id));
    }

    @PatchMapping("/{id}/name")
    @Operation(summary = "Rename an artist",
            description = "Partial update — changes the artist's canonical name. Emits an ARTIST_NAME_CHANGED audit event.")
    @ApiResponse(responseCode = "200", description = "Name updated",
            content = @Content(schema = @Schema(implementation = ArtistResponse.class)))
    @ApiResponse(responseCode = "404", description = "Artist not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "400", description = "Validation failure",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Concurrent modification conflict (optimistic lock)",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ArtistResponse editArtistName(
            @PathVariable UUID id,
            @Valid @RequestBody EditArtistNameRequest request
    ) {
        return ArtistResponse.from(editArtistNameUseCase.editName(id, request.name()));
    }

    @GetMapping(params = "name")
    @Operation(summary = "Search artists by name",
            description = "Searches canonical names and aliases. Returns all matching artists (deduplicated).")
    @ApiResponse(responseCode = "200", description = "Search results (may be empty)")
    public List<ArtistResponse> findByName(@RequestParam String name) {
        return findArtistUseCase.findByName(name).stream()
                .map(ArtistResponse::from)
                .toList();
    }

    @PostMapping("/{id}/aliases")
    @Idempotent(namespace = "artist:alias")
    @Operation(summary = "Add an alias for an artist",
            description = "Registers an alternative name (e.g. 'Ziggy Stardust' for David Bowie). "
                    + "Aliases are included in name searches. Supports X-Idempotency-Key.")
    @ApiResponse(responseCode = "201", description = "Alias created",
            content = @Content(schema = @Schema(implementation = AliasResponse.class)))
    @ApiResponse(responseCode = "404", description = "Artist not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "400", description = "Validation failure",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Idempotency conflict",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<AliasResponse> addAlias(
            @PathVariable UUID id,
            @Valid @RequestBody AddAliasRequest request
    ) {
        var response = AliasResponse.from(addAliasUseCase.addAlias(id, request.alias()));
        return ResponseEntity
                .created(URI.create("/api/artists/" + id + "/aliases/" + response.id()))
                .body(response);
    }
}
