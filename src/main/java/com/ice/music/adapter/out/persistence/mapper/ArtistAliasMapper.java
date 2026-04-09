package com.ice.music.adapter.out.persistence.mapper;

import com.ice.music.adapter.out.persistence.entity.ArtistAliasEntity;
import com.ice.music.domain.model.ArtistAlias;
import org.springframework.stereotype.Component;

@Component
public class ArtistAliasMapper {

    public ArtistAliasEntity toNewEntity(ArtistAlias alias) {
        return ArtistAliasEntity.forInsert(alias.id(), alias.artistId(), alias.aliasName());
    }

    public ArtistAlias toDomain(ArtistAliasEntity entity) {
        return new ArtistAlias(
                entity.getId(),
                entity.getArtistId(),
                entity.getAliasName(),
                entity.getCreatedAt()
        );
    }
}
