package com.ice.music.port.out;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Outbound port for cache operations.
 *
 * Backed by Redis in all environments. Supports pub/sub
 * for event-driven coordination (e.g. AOD single-flight).
 */
public interface CachePort {

    Optional<String> get(String key);

    void set(String key, String value, Duration ttl);

    /**
     * Set a value with no expiry. Persists until explicitly deleted
     * or Redis restarts without persistence.
     */
    void set(String key, String value);

    void delete(String key);

    /**
     * Atomic increment (Redis INCR). Returns the value after increment.
     * Creates the key with value 1 if it does not exist.
     */
    long increment(String key);

    /**
     * Set only if the key does not exist (SETNX).
     * Returns true if the key was set, false if it already existed.
     */
    boolean setIfAbsent(String key, String value, Duration ttl);

    /**
     * Publish a message to a channel.
     */
    void publish(String channel, String message);

    /**
     * Subscribe to a channel and complete the future when the first message arrives.
     * The subscription is automatically removed after delivery.
     */
    CompletableFuture<String> subscribeToOnce(String channel);
}
