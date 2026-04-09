package com.ice.music.adapter.in.web.dto;

import com.ice.music.domain.model.Artist;

import java.time.LocalDate;

public record ArtistOfTheDayResponse(
        ArtistResponse artist,
        LocalDate date
) {
    public static ArtistOfTheDayResponse from(Artist artist, LocalDate date) {
        return new ArtistOfTheDayResponse(ArtistResponse.from(artist), date);
    }
}
