package com.ice.music.port.out;

/**
 * Outbound port for identifying the current actor.
 *
 * The domain asks "who is acting?" without knowing whether the
 * answer comes from a request header, OAuth2 token, or API key.
 *
 * POC: reads from X-Actor-Id header via ScopedValue.
 * Production: reads from SecurityContextHolder.
 */
public interface ActorContext {

    String currentActorId();
}
