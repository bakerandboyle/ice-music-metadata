package com.ice.music.adapter.in.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method as idempotent.
 *
 * The IdempotencyAspect intercepts annotated methods and checks
 * the X-Idempotency-Key header against Redis. Duplicate requests
 * receive the cached response without re-executing business logic.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    String namespace() default "general";

    long ttlSeconds() default 3600;
}
