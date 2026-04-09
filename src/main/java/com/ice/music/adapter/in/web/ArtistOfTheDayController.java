package com.ice.music.adapter.in.web;

import com.ice.music.adapter.in.web.dto.ArtistOfTheDayResponse;
import com.ice.music.port.in.ArtistOfTheDayUseCase;
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
@RequestMapping("/api/artist-of-the-day")
public class ArtistOfTheDayController {

    private final ArtistOfTheDayUseCase artistOfTheDayUseCase;
    private final Clock clock;

    public ArtistOfTheDayController(ArtistOfTheDayUseCase artistOfTheDayUseCase, Clock clock) {
        this.artistOfTheDayUseCase = artistOfTheDayUseCase;
        this.clock = clock;
    }

    @GetMapping
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
