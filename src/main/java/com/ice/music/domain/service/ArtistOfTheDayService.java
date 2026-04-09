package com.ice.music.domain.service;

import com.ice.music.domain.model.Artist;
import com.ice.music.port.in.ArtistOfTheDayUseCase;
import com.ice.music.port.out.ArtistRepository;
import com.ice.music.port.out.CachePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Artist of the Day engine.
 *
 * Algorithm: epochDay % totalArtistCount → Rank-Pointer OFFSET query.
 * Cache: Redis with single-flight via SETNX + pub/sub signal.
 * Anchor: UTC midnight — globally consistent (see README Decision #2).
 */
@Service
@Transactional(readOnly = true)
public class ArtistOfTheDayService implements ArtistOfTheDayUseCase {

    private static final Logger log = LoggerFactory.getLogger(ArtistOfTheDayService.class);
    private static final String ARTIST_COUNT_KEY = "artist:count";
    private static final String CACHE_KEY = "aod:artist";
    private static final String LOCK_KEY = "aod:lock";
    private static final String NOTIFICATION_CHANNEL = "aod:notifications";
    private static final Duration LOCK_TTL = Duration.ofSeconds(30);
    private static final long SIGNAL_TIMEOUT_SECONDS = 2;

    private final ArtistRepository artistRepository;
    private final CachePort cache;
    private final Clock clock;

    public ArtistOfTheDayService(ArtistRepository artistRepository, CachePort cache, Clock clock) {
        this.artistRepository = artistRepository;
        this.cache = cache;
        this.clock = clock;
    }

    @Override
    public Optional<Artist> getArtistOfTheDay() {
        return cache.get(CACHE_KEY)
                .flatMap(this::resolveArtistById)
                .or(this::computeAndCache);
    }

    private Optional<Artist> resolveArtistById(String cachedId) {
        return artistRepository.findById(UUID.fromString(cachedId));
    }

    private Optional<Artist> computeAndCache() {
        if (cache.setIfAbsent(LOCK_KEY, "computing", LOCK_TTL)) {
            try {
                var artist = compute();
                cache.publish(NOTIFICATION_CHANNEL, "ready");
                return artist;
            } finally {
                cache.delete(LOCK_KEY);
            }
        }
        return waitForSignal();
    }

    private Optional<Artist> compute() {
        var totalArtists = getArtistCount();
        if (totalArtists == 0) {
            return Optional.empty();
        }

        var epochDay = LocalDate.now(clock).toEpochDay();
        var targetIndex = Math.floorMod(epochDay, totalArtists);

        return artistRepository.findByRank(targetIndex)
                .map(artist -> {
                    cache.set(CACHE_KEY, artist.id().toString(), ttlToMidnight());
                    log.info("AOD computed: {} (index={}, total={})", artist.name(), targetIndex, totalArtists);
                    return artist;
                });
    }

    private Optional<Artist> waitForSignal() {
        var signal = cache.subscribeToOnce(NOTIFICATION_CHANNEL);
        try {
            signal.get(SIGNAL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            log.warn("AOD signal interrupted — falling back to direct compute");
            return compute();
        } catch (TimeoutException | ExecutionException _) {
            log.warn("AOD signal timeout — falling back to direct compute");
            return compute();
        }
        return cache.get(CACHE_KEY).flatMap(this::resolveArtistById);
    }

    /**
     * Read artist count from Redis. On cold start (key missing),
     * seed from DB — this is the only time COUNT(*) runs.
     */
    private long getArtistCount() {
        return cache.get(ARTIST_COUNT_KEY)
                .map(Long::parseLong)
                .orElseGet(this::seedCountFromDb);
    }

    private long seedCountFromDb() {
        var count = artistRepository.count();
        if (count > 0) {
            cache.set(ARTIST_COUNT_KEY, String.valueOf(count));
            log.info("AOD seeded artist count from DB: {}", count);
        }
        return count;
    }

    private Duration ttlToMidnight() {
        var now = LocalTime.now(clock);
        var secondsUntilMidnight = Duration.between(now, LocalTime.MAX).getSeconds() + 1;
        return Duration.ofSeconds(Math.max(secondsUntilMidnight, 60));
    }
}
