package com.ice.music.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EditArtistNameRequest(
        @NotBlank(message = "Artist name is required")
        @Size(max = 500, message = "Artist name must not exceed 500 characters")
        String name
) {
}
