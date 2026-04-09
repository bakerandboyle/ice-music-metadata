package com.ice.music.adapter.out.cache;

import com.ice.music.port.out.CachePort;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Redis adapter implementing the CachePort.
 *
 * Pub/sub enables event-driven coordination: the AOD single-flight
 * winner publishes a signal, waiters complete immediately without polling.
 */
@Component
public class RedisCacheAdapter implements CachePort {

    private final StringRedisTemplate redis;
    private final RedisMessageListenerContainer listenerContainer;

    public RedisCacheAdapter(StringRedisTemplate redis, RedisMessageListenerContainer listenerContainer) {
        this.redis = redis;
        this.listenerContainer = listenerContainer;
    }

    @Override
    public Optional<String> get(String key) {
        return Optional.ofNullable(redis.opsForValue().get(key));
    }

    @Override
    public void set(String key, String value, Duration ttl) {
        redis.opsForValue().set(key, value, ttl);
    }

    @Override
    public void set(String key, String value) {
        redis.opsForValue().set(key, value);
    }

    @Override
    public void delete(String key) {
        redis.delete(key);
    }

    @Override
    public long increment(String key) {
        return Optional.ofNullable(redis.opsForValue().increment(key))
                .orElseThrow(() -> new IllegalStateException("Redis INCR returned null for key: " + key));
    }

    @Override
    public boolean setIfAbsent(String key, String value, Duration ttl) {
        return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, value, ttl));
    }

    @Override
    public void publish(String channel, String message) {
        redis.convertAndSend(channel, message);
    }

    @Override
    public CompletableFuture<String> subscribeToOnce(String channel) {
        var future = new CompletableFuture<String>();
        var topic = new ChannelTopic(channel);

        MessageListener listener = new MessageListener() {
            @Override
            public void onMessage(Message message, byte[] pattern) {
                future.complete(new String(message.getBody()));
                listenerContainer.removeMessageListener(this, topic);
            }
        };

        listenerContainer.addMessageListener(listener, topic);
        return future;
    }
}
