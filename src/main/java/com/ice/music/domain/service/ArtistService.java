package com.ice.music.domain.service;

import com.ice.music.domain.model.Artist;
import com.ice.music.domain.model.ArtistAlias;
import com.ice.music.domain.model.ArtistNotFoundException;
import com.ice.music.domain.model.AuditEvent;
import com.ice.music.port.in.AddAliasUseCase;
import com.ice.music.port.in.CreateArtistUseCase;
import com.ice.music.port.in.EditArtistNameUseCase;
import com.ice.music.port.in.FindArtistUseCase;
import com.ice.music.port.out.ActorContext;
import com.ice.music.port.out.AliasRepository;
import com.ice.music.port.out.ArtistRepository;
import com.ice.music.port.out.AuditPublisher;
import com.ice.music.port.out.CachePort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@Transactional
public class ArtistService implements CreateArtistUseCase, EditArtistNameUseCase, FindArtistUseCase, AddAliasUseCase {

    static final String ARTIST_COUNT_KEY = "artist:count";

    private final ArtistRepository artistRepository;
    private final AliasRepository aliasRepository;
    private final CachePort cache;
    private final AuditPublisher auditPublisher;
    private final ActorContext actorContext;

    public ArtistService(ArtistRepository artistRepository, AliasRepository aliasRepository,
                         CachePort cache, AuditPublisher auditPublisher, ActorContext actorContext) {
        this.artistRepository = artistRepository;
        this.aliasRepository = aliasRepository;
        this.cache = cache;
        this.auditPublisher = auditPublisher;
        this.actorContext = actorContext;
    }

    @Override
    public Artist create(String name) {
        var artist = artistRepository.save(Artist.create(name));
        cache.increment(ARTIST_COUNT_KEY);
        auditPublisher.publish(AuditEvent.artistCreated(artist, actorContext.currentActorId()));
        return artist;
    }

    @Override
    public Artist editName(UUID artistId, String newName) {
        var before = artistRepository.findById(artistId)
                .orElseThrow(() -> new ArtistNotFoundException(artistId));

        var after = artistRepository.save(before.withName(newName));

        auditPublisher.publish(AuditEvent.artistNameUpdated(
                artistId, before.name(), after.name(), actorContext.currentActorId()));
        return after;
    }

    @Override
    @Transactional(readOnly = true)
    public Artist findById(UUID id) {
        return artistRepository.findById(id)
                .orElseThrow(() -> new ArtistNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Artist> findByName(String name) {
        var byCanonicalName = artistRepository.findByNameIgnoreCase(name);

        var byAlias = aliasRepository.findArtistIdsByAliasIgnoreCase(name).stream()
                .map(artistRepository::findById)
                .flatMap(java.util.Optional::stream);

        return Stream.concat(byCanonicalName.stream(), byAlias)
                .distinct()
                .toList();
    }

    @Override
    public ArtistAlias addAlias(UUID artistId, String aliasName) {
        artistRepository.findById(artistId)
                .orElseThrow(() -> new ArtistNotFoundException(artistId));

        var alias = aliasRepository.save(ArtistAlias.create(artistId, aliasName));
        auditPublisher.publish(AuditEvent.artistAliasAdded(
                artistId, alias, actorContext.currentActorId()));
        return alias;
    }
}
