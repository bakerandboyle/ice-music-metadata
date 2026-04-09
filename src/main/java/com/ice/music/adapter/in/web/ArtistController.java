package com.ice.music.adapter.in.web;

import com.ice.music.adapter.in.web.dto.ArtistResponse;
import com.ice.music.adapter.in.web.dto.CreateArtistRequest;
import com.ice.music.adapter.in.web.dto.EditArtistNameRequest;
import com.ice.music.port.in.CreateArtistUseCase;
import com.ice.music.port.in.EditArtistNameUseCase;
import com.ice.music.port.in.FindArtistUseCase;
import jakarta.validation.Valid;
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
 *
 * Versioning: X-ICE-Version header (date-based).
 * Current version (2026-04-09) is the default when header is omitted.
 */
@RestController
@RequestMapping("/api/artists")
public class ArtistController {

    private final CreateArtistUseCase createArtistUseCase;
    private final EditArtistNameUseCase editArtistNameUseCase;
    private final FindArtistUseCase findArtistUseCase;

    public ArtistController(
            CreateArtistUseCase createArtistUseCase,
            EditArtistNameUseCase editArtistNameUseCase,
            FindArtistUseCase findArtistUseCase
    ) {
        this.createArtistUseCase = createArtistUseCase;
        this.editArtistNameUseCase = editArtistNameUseCase;
        this.findArtistUseCase = findArtistUseCase;
    }

    @PostMapping
    public ResponseEntity<ArtistResponse> createArtist(@Valid @RequestBody CreateArtistRequest request) {
        var response = ArtistResponse.from(createArtistUseCase.create(request.name()));
        return ResponseEntity
                .created(URI.create("/api/artists/" + response.id()))
                .body(response);
    }

    @GetMapping("/{id}")
    public ArtistResponse getArtist(@PathVariable UUID id) {
        return ArtistResponse.from(findArtistUseCase.findById(id));
    }

    @PatchMapping("/{id}/name")
    public ArtistResponse editArtistName(
            @PathVariable UUID id,
            @Valid @RequestBody EditArtistNameRequest request
    ) {
        return ArtistResponse.from(editArtistNameUseCase.editName(id, request.name()));
    }

    @GetMapping(params = "name")
    public List<ArtistResponse> findByName(@RequestParam String name) {
        return findArtistUseCase.findByName(name).stream()
                .map(ArtistResponse::from)
                .toList();
    }
}
