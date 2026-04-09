package com.ice.music.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateTrackRequest(
        @NotBlank(message = "Track title is required")
        @Size(max = 1000, message = "Track title must not exceed 1000 characters")
        String title,

        @NotBlank(message = "ISRC is required")
        @Size(min = 12, max = 12, message = "ISRC must be exactly 12 characters")
        String isrc,

        String genre,

        @Positive(message = "Duration must be positive")
        int durationSeconds
) {
}
