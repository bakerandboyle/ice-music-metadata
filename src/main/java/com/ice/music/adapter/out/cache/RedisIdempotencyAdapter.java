package com.ice.music.adapter.out.cache;

import com.ice.music.port.out.IdempotencyPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis adapter for idempotency key management.
 *
 * Uses two keys per idempotency request:
 * - lock:{key} — SETNX claim for in-flight deduplication
 * - response:{key} — stored response for completed requests
 */
@Component
public class RedisIdempotencyAdapter implements IdempotencyPort {

    private static final String LOCK_PREFIX = "idempotency:lock:";
    private static final String RESPONSE_PREFIX = "idempotency:response:";

    private final StringRedisTemplate redis;

    public RedisIdempotencyAdapter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public boolean claim(String key, Duration ttl) {
        return Boolean.TRUE.equals(
                redis.opsForValue().setIfAbsent(LOCK_PREFIX + key, "processing", ttl));
    }

    @Override
    public void storeResponse(String key, String serializedResponse, Duration ttl) {
        redis.opsForValue().set(RESPONSE_PREFIX + key, serializedResponse, ttl);
    }

    @Override
    public Optional<String> getResponse(String key) {
        return Optional.ofNullable(redis.opsForValue().get(RESPONSE_PREFIX + key));
    }
}
