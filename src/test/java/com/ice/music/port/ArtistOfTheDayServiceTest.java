package com.ice.music.port;

import com.ice.music.domain.model.Artist;
import com.ice.music.domain.service.ArtistOfTheDayService;
import com.ice.music.port.out.ArtistRepository;
import com.ice.music.port.out.CachePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArtistOfTheDayServiceTest {

    private static final LocalDate FIXED_DATE = LocalDate.of(2026, 4, 9);
    private static final Clock FIXED_CLOCK = Clock.fixed(
            FIXED_DATE.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);

    @Mock
    private ArtistRepository artistRepository;

    @Mock
    private CachePort cache;

    private ArtistOfTheDayService service() {
        return new ArtistOfTheDayService(artistRepository, cache, FIXED_CLOCK);
    }

    @Test
    void returnsCachedArtistOnCacheHit() {
        var artist = Artist.create("Queen");
        when(cache.get("aod:artist")).thenReturn(Optional.of(artist.id().toString()));
        when(artistRepository.findById(artist.id())).thenReturn(Optional.of(artist));

        var result = service().getArtistOfTheDay();

        assertThat(result).isPresent()
                .get()
                .satisfies(a -> assertThat(a.name()).isEqualTo("Queen"));
        verify(cache, never()).get("artist:count");
    }

    @Test
    void computesFromRedisCounterOnCacheMiss() {
        var artist = Artist.create("Queen");
        when(cache.get("aod:artist")).thenReturn(Optional.empty());
        when(cache.get("artist:count")).thenReturn(Optional.of("1"));
        when(cache.setIfAbsent(eq("aod:lock"), anyString(), any(Duration.class))).thenReturn(true);
        when(artistRepository.findByRank(any(Long.class))).thenReturn(Optional.of(artist));

        var result = service().getArtistOfTheDay();

        assertThat(result).isPresent();
        verify(cache).set(eq("aod:artist"), eq(artist.id().toString()), any(Duration.class));
        verify(cache).publish("aod:notifications", "ready");
        verify(cache).delete("aod:lock");
        verify(artistRepository, never()).count();
    }

    @Test
    void seedsCountFromDbOnColdStart() {
        var artist = Artist.create("Queen");
        when(cache.get("aod:artist")).thenReturn(Optional.empty());
        when(cache.get("artist:count")).thenReturn(Optional.empty());
        when(cache.setIfAbsent(eq("aod:lock"), anyString(), any(Duration.class))).thenReturn(true);
        when(artistRepository.count()).thenReturn(1L);
        when(artistRepository.findByRank(any(Long.class))).thenReturn(Optional.of(artist));

        var result = service().getArtistOfTheDay();

        assertThat(result).isPresent();
        verify(artistRepository).count();
        verify(cache).set("artist:count", "1");
    }

    @Test
    void returnsEmptyWhenCatalogueEmpty() {
        when(cache.get("aod:artist")).thenReturn(Optional.empty());
        when(cache.get("artist:count")).thenReturn(Optional.empty());
        when(cache.setIfAbsent(eq("aod:lock"), anyString(), any(Duration.class))).thenReturn(true);
        when(artistRepository.count()).thenReturn(0L);

        var result = service().getArtistOfTheDay();

        assertThat(result).isEmpty();
    }

    @Test
    void usesModuloForDeterministicRotation() {
        var epochDay = FIXED_DATE.toEpochDay();
        var totalArtists = 5L;
        var expectedIndex = Math.floorMod(epochDay, totalArtists);

        var artist = Artist.create("Artist " + expectedIndex);
        when(cache.get("aod:artist")).thenReturn(Optional.empty());
        when(cache.get("artist:count")).thenReturn(Optional.of(String.valueOf(totalArtists)));
        when(cache.setIfAbsent(eq("aod:lock"), anyString(), any(Duration.class))).thenReturn(true);
        when(artistRepository.findByRank(expectedIndex)).thenReturn(Optional.of(artist));

        var result = service().getArtistOfTheDay();

        assertThat(result).isPresent();
        verify(artistRepository).findByRank(expectedIndex);
    }

    @Test
    void waitsForSignalWhenLockNotAcquired() {
        var artist = Artist.create("Queen");
        when(cache.get("aod:artist"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(artist.id().toString()));
        when(cache.setIfAbsent(eq("aod:lock"), anyString(), any(Duration.class))).thenReturn(false);
        when(cache.subscribeToOnce("aod:notifications"))
                .thenReturn(CompletableFuture.completedFuture("ready"));
        when(artistRepository.findById(artist.id())).thenReturn(Optional.of(artist));

        var result = service().getArtistOfTheDay();

        assertThat(result).isPresent();
        verify(artistRepository, never()).count();
    }
}
