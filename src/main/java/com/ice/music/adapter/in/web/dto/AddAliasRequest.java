package com.ice.music.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddAliasRequest(
        @NotBlank(message = "Alias name is required")
        @Size(max = 500, message = "Alias name must not exceed 500 characters")
        String alias
) {
}
