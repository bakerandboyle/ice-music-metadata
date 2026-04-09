package com.ice.music.adapter.in.web.dto;

import com.ice.music.domain.model.ArtistAlias;

import java.time.Instant;
import java.util.UUID;

public record AliasResponse(
        UUID id,
        UUID artistId,
        String aliasName,
        Instant createdAt
) {
    public static AliasResponse from(ArtistAlias alias) {
        return new AliasResponse(alias.id(), alias.artistId(), alias.aliasName(), alias.createdAt());
    }
}
