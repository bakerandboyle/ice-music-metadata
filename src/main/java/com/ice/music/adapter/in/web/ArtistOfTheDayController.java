package com.ice.music.adapter.in.web;

import com.ice.music.adapter.in.web.dto.ArtistOfTheDayResponse;
import com.ice.music.port.in.ArtistOfTheDayUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Clock;
import java.time.LocalDate;

/**
 * REST adapter for Artist of the Day.
 */
@RestController
@RequestMapping(path = "/api/artist-of-the-day", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Artist of the Day", description = "Daily featured artist — deterministic rotation via Rank-Pointer algorithm")
public class ArtistOfTheDayController {

    private final ArtistOfTheDayUseCase artistOfTheDayUseCase;
    private final Clock clock;

    public ArtistOfTheDayController(ArtistOfTheDayUseCase artistOfTheDayUseCase, Clock clock) {
        this.artistOfTheDayUseCase = artistOfTheDayUseCase;
        this.clock = clock;
    }

    @GetMapping
    @Operation(summary = "Get today's featured artist",
            description = "Returns the Artist of the Day based on a deterministic Rank-Pointer rotation "
                    + "(epochDay mod artistCount). Anchored to UTC midnight. "
                    + "Redis-cached with pub/sub single-flight protection.")
    @ApiResponse(responseCode = "200", description = "Artist of the Day",
            content = @Content(schema = @Schema(implementation = ArtistOfTheDayResponse.class)))
    @ApiResponse(responseCode = "404", description = "Catalogue is empty — no artists available",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<ArtistOfTheDayResponse> getArtistOfTheDay() {
        return artistOfTheDayUseCase.getArtistOfTheDay()
                .map(artist -> ResponseEntity.ok(
                        ArtistOfTheDayResponse.from(artist, LocalDate.now(clock))))
                .orElseGet(() -> {
                    var problem = ProblemDetail.forStatus(404);
                    problem.setTitle("No Artists Available");
                    problem.setDetail("The catalogue is empty. No Artist of the Day is available.");
                    problem.setType(URI.create("https://ice.com/problems/no-artists"));
                    return ResponseEntity.of(problem).build();
                });
    }
}
