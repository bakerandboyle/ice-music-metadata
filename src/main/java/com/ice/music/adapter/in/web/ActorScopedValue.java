package com.ice.music.adapter.in.web;

/**
 * Holds the ScopedValue for actor identity.
 *
 * Java 25 ScopedValues are the virtual-thread-safe replacement for
 * ThreadLocal. Zero proxy overhead, inherently bound to the carrier
 * thread's scope, and cannot leak across virtual thread boundaries.
 */
public final class ActorScopedValue {

    public static final ScopedValue<String> ACTOR_ID = ScopedValue.newInstance();

    private ActorScopedValue() {
    }
}
