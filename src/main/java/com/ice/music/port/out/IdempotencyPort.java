package com.ice.music.port.out;

import java.time.Duration;
import java.util.Optional;

/**
 * Outbound port for idempotency key management.
 *
 * Separate from CachePort — idempotency is a distinct concern
 * with its own semantics (claim/store/retrieve vs generic get/set).
 */
public interface IdempotencyPort {

    /**
     * Atomically claim a request key (SETNX).
     * Returns true if this is a new request, false if already claimed.
     * Acts as a distributed lock for in-flight deduplication.
     */
    boolean claim(String key, Duration ttl);

    /**
     * Store the serialized response for a completed request.
     */
    void storeResponse(String key, String serializedResponse, Duration ttl);

    /**
     * Retrieve a previously stored response.
     */
    Optional<String> getResponse(String key);
}
