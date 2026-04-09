package com.ice.music.adapter.in.web;

import com.ice.music.domain.model.ArtistNotFoundException;
import com.ice.music.domain.model.Track;
import com.ice.music.port.in.AddTrackUseCase;
import com.ice.music.port.in.FetchArtistTracksUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TrackController.class)
class TrackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AddTrackUseCase addTrackUseCase;

    @MockitoBean
    private FetchArtistTracksUseCase fetchArtistTracksUseCase;

    private static final UUID ARTIST_ID = UUID.randomUUID();

    // --- POST /api/artists/{artistId}/tracks ---

    @Test
    void addTrack_returns201WithLocationHeader() throws Exception {
        var track = Track.create(ARTIST_ID, "Bohemian Rhapsody", "GBAYE7500101", "Rock", 354);
        when(addTrackUseCase.addTrack(eq(ARTIST_ID), anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(track);

        mockMvc.perform(post("/api/artists/{artistId}/tracks", ARTIST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Bohemian Rhapsody",
                                    "isrc": "GBAYE7500101",
                                    "genre": "Rock",
                                    "durationSeconds": 354
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.title", is("Bohemian Rhapsody")))
                .andExpect(jsonPath("$.isrc", is("GBAYE7500101")))
                .andExpect(jsonPath("$.artistId", is(ARTIST_ID.toString())));
    }

    @Test
    void addTrack_returns404WhenArtistNotFound() throws Exception {
        when(addTrackUseCase.addTrack(eq(ARTIST_ID), anyString(), anyString(), anyString(), anyInt()))
                .thenThrow(new ArtistNotFoundException(ARTIST_ID));

        mockMvc.perform(post("/api/artists/{artistId}/tracks", ARTIST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Title",
                                    "isrc": "GBAYE7500101",
                                    "genre": "Rock",
                                    "durationSeconds": 354
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title", is("Artist Not Found")));
    }

    @Test
    void addTrack_returns400WhenTitleMissing() throws Exception {
        mockMvc.perform(post("/api/artists/{artistId}/tracks", ARTIST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "isrc": "GBAYE7500101",
                                    "genre": "Rock",
                                    "durationSeconds": 354
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addTrack_returns400WhenIsrcWrongLength() throws Exception {
        mockMvc.perform(post("/api/artists/{artistId}/tracks", ARTIST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Title",
                                    "isrc": "SHORT",
                                    "genre": "Rock",
                                    "durationSeconds": 354
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    // --- GET /api/artists/{artistId}/tracks ---

    @Test
    void fetchTracks_returns200WithTrackList() throws Exception {
        var track = Track.create(ARTIST_ID, "Bohemian Rhapsody", "GBAYE7500101", "Rock", 354);
        when(fetchArtistTracksUseCase.fetchTracks(ARTIST_ID)).thenReturn(List.of(track));

        mockMvc.perform(get("/api/artists/{artistId}/tracks", ARTIST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Bohemian Rhapsody")))
                .andExpect(jsonPath("$[0].isrc", is("GBAYE7500101")));
    }

    @Test
    void fetchTracks_returns200EmptyListWhenNoTracks() throws Exception {
        when(fetchArtistTracksUseCase.fetchTracks(ARTIST_ID)).thenReturn(List.of());

        mockMvc.perform(get("/api/artists/{artistId}/tracks", ARTIST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void fetchTracks_returns404WhenArtistNotFound() throws Exception {
        when(fetchArtistTracksUseCase.fetchTracks(ARTIST_ID))
                .thenThrow(new ArtistNotFoundException(ARTIST_ID));

        mockMvc.perform(get("/api/artists/{artistId}/tracks", ARTIST_ID))
                .andExpect(status().isNotFound());
    }
}
