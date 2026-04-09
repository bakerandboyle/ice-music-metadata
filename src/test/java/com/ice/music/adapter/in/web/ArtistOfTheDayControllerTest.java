package com.ice.music.adapter.in.web;

import com.ice.music.domain.model.Artist;
import com.ice.music.port.in.ArtistOfTheDayUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ArtistOfTheDayController.class)
@Import(ArtistOfTheDayControllerTest.FixedClockConfig.class)
class ArtistOfTheDayControllerTest {

    private static final LocalDate FIXED_DATE = LocalDate.of(2026, 4, 9);

    static class FixedClockConfig {
        @Bean
        public Clock clock() {
            return Clock.fixed(FIXED_DATE.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArtistOfTheDayUseCase artistOfTheDayUseCase;

    @Test
    void returns200WithArtistAndDate() throws Exception {
        var artist = Artist.create("Queen");
        when(artistOfTheDayUseCase.getArtistOfTheDay()).thenReturn(Optional.of(artist));

        mockMvc.perform(get("/api/artist-of-the-day"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.artist.name", is("Queen")))
                .andExpect(jsonPath("$.date", is("2026-04-09")));
    }

    @Test
    void returns404WhenCatalogueEmpty() throws Exception {
        when(artistOfTheDayUseCase.getArtistOfTheDay()).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/artist-of-the-day"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title", is("No Artists Available")));
    }
}
