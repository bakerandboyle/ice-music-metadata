package com.ice.music.adapter.in.web;

import com.ice.music.port.out.ActorContext;
import org.springframework.stereotype.Component;

/**
 * Reads the current actor identity from the ScopedValue
 * bound by ActorFilter at the start of the request.
 *
 * No proxying, no ThreadLocal, no request-scope bean lifecycle.
 * ScopedValue is immutable within scope and inherently safe
 * on virtual threads — the correct choice for Java 25 LTS.
 */
@Component
public class ScopedActorContext implements ActorContext {

    private static final String ANONYMOUS = "anonymous";

    @Override
    public String currentActorId() {
        return ActorScopedValue.ACTOR_ID.isBound()
                ? ActorScopedValue.ACTOR_ID.get()
                : ANONYMOUS;
    }
}
