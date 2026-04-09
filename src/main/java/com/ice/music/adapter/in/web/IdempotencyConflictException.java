package com.ice.music.adapter.in.web;

/**
 * Thrown when an idempotency key is currently being processed
 * by another request (in-flight duplicate).
 */
public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(String message) {
        super(message);
    }
}
