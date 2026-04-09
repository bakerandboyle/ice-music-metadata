package com.ice.music.adapter.out.persistence;

import com.ice.music.adapter.out.persistence.mapper.ArtistAliasMapper;
import com.ice.music.adapter.out.persistence.repository.SpringDataArtistAliasRepository;
import com.ice.music.domain.model.ArtistAlias;
import com.ice.music.port.out.AliasRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class JpaAliasRepository implements AliasRepository {

    private final SpringDataArtistAliasRepository springDataRepo;
    private final ArtistAliasMapper mapper;

    public JpaAliasRepository(SpringDataArtistAliasRepository springDataRepo, ArtistAliasMapper mapper) {
        this.springDataRepo = springDataRepo;
        this.mapper = mapper;
    }

    @Override
    public ArtistAlias save(ArtistAlias alias) {
        var entity = mapper.toNewEntity(alias);
        var saved = springDataRepo.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<UUID> findArtistIdsByAliasIgnoreCase(String aliasName) {
        return springDataRepo.findArtistIdsByAliasNameIgnoreCase(aliasName);
    }
}
