package com.ice.music.adapter.in.web;

import com.ice.music.domain.model.Artist;
import com.ice.music.domain.model.ArtistAlias;
import com.ice.music.domain.model.ArtistNotFoundException;
import com.ice.music.port.in.AddAliasUseCase;
import com.ice.music.port.in.CreateArtistUseCase;
import com.ice.music.port.in.EditArtistNameUseCase;
import com.ice.music.port.in.FindArtistUseCase;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ArtistController.class)
class ArtistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateArtistUseCase createArtistUseCase;

    @MockitoBean
    private EditArtistNameUseCase editArtistNameUseCase;

    @MockitoBean
    private FindArtistUseCase findArtistUseCase;

    @MockitoBean
    private AddAliasUseCase addAliasUseCase;

    // --- POST /api/artists ---

    @Test
    void createArtist_returns201WithLocationHeader() throws Exception {
        var artist = Artist.create("Queen");
        when(createArtistUseCase.create("Queen")).thenReturn(artist);

        mockMvc.perform(post("/api/artists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Queen"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/artists/" + artist.id()))
                .andExpect(jsonPath("$.id", is(artist.id().toString())))
                .andExpect(jsonPath("$.name", is("Queen")));
    }

    @Test
    void createArtist_returns400WhenNameBlank() throws Exception {
        mockMvc.perform(post("/api/artists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": ""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createArtist_returns400WhenNameMissing() throws Exception {
        mockMvc.perform(post("/api/artists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // --- GET /api/artists/{id} ---

    @Test
    void getArtist_returns200WithArtist() throws Exception {
        var artist = Artist.create("Queen");
        when(findArtistUseCase.findById(artist.id())).thenReturn(artist);

        mockMvc.perform(get("/api/artists/{id}", artist.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Queen")));
    }

    @Test
    void getArtist_returns404WhenNotFound() throws Exception {
        var missingId = UUID.randomUUID();
        when(findArtistUseCase.findById(missingId))
                .thenThrow(new ArtistNotFoundException(missingId));

        mockMvc.perform(get("/api/artists/{id}", missingId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title", is("Artist Not Found")));
    }

    // --- PATCH /api/artists/{id}/name ---

    @Test
    void editArtistName_returns200WithUpdatedArtist() throws Exception {
        var artist = Artist.create("Queen");
        var renamed = artist.withName("Freddie Mercury");
        when(editArtistNameUseCase.editName(artist.id(), "Freddie Mercury"))
                .thenReturn(renamed);

        mockMvc.perform(patch("/api/artists/{id}/name", artist.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Freddie Mercury"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Freddie Mercury")));
    }

    @Test
    void editArtistName_returns404WhenNotFound() throws Exception {
        var missingId = UUID.randomUUID();
        when(editArtistNameUseCase.editName(eq(missingId), any()))
                .thenThrow(new ArtistNotFoundException(missingId));

        mockMvc.perform(patch("/api/artists/{id}/name", missingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "New Name"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void editArtistName_returns400WhenNameBlank() throws Exception {
        mockMvc.perform(patch("/api/artists/{id}/name", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "  "}
                                """))
                .andExpect(status().isBadRequest());
    }

    // --- GET /api/artists?name={name} ---

    @Test
    void findByName_returns200WithMatchingArtists() throws Exception {
        var queen = Artist.create("Queen");
        when(findArtistUseCase.findByName("Queen")).thenReturn(List.of(queen));

        mockMvc.perform(get("/api/artists").param("name", "Queen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Queen")));
    }

    @Test
    void findByName_returns200WithEmptyListWhenNoneMatch() throws Exception {
        when(findArtistUseCase.findByName("Nobody")).thenReturn(List.of());

        mockMvc.perform(get("/api/artists").param("name", "Nobody"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // --- POST /api/artists/{id}/aliases ---

    @Test
    void addAlias_returns201WithLocationHeader() throws Exception {
        var artistId = UUID.randomUUID();
        var alias = ArtistAlias.create(artistId, "Farrokh Bulsara");
        when(addAliasUseCase.addAlias(artistId, "Farrokh Bulsara")).thenReturn(alias);

        mockMvc.perform(post("/api/artists/{id}/aliases", artistId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"alias": "Farrokh Bulsara"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.aliasName", is("Farrokh Bulsara")))
                .andExpect(jsonPath("$.artistId", is(artistId.toString())));
    }

    @Test
    void addAlias_returns404WhenArtistNotFound() throws Exception {
        var missingId = UUID.randomUUID();
        when(addAliasUseCase.addAlias(eq(missingId), any()))
                .thenThrow(new ArtistNotFoundException(missingId));

        mockMvc.perform(post("/api/artists/{id}/aliases", missingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"alias": "Some Alias"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void addAlias_returns400WhenAliasBlank() throws Exception {
        mockMvc.perform(post("/api/artists/{id}/aliases", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"alias": "  "}
                                """))
                .andExpect(status().isBadRequest());
    }
}
