package com.ice.music.adapter.in.web;

import com.ice.music.port.out.IdempotencyPort;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.Optional;

/**
 * Intercepts @Idempotent controller methods.
 *
 * Flow:
 * 1. No header → proceed normally
 * 2. Cached response → return cached (zero side effects)
 * 3. Claim succeeds → proceed, cache result, return
 * 4. Claim fails (in-flight) → 409 Conflict
 *
 * Lives in the adapter layer — the domain is unaware of deduplication.
 */
@Aspect
@Component
public class IdempotencyAspect {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyAspect.class);
    private static final String HEADER = "X-Idempotency-Key";

    private final IdempotencyPort idempotencyPort;
    private final HttpServletRequest request;
    private final JsonMapper jsonMapper;

    public IdempotencyAspect(IdempotencyPort idempotencyPort, HttpServletRequest request, JsonMapper jsonMapper) {
        this.idempotencyPort = idempotencyPort;
        this.request = request;
        this.jsonMapper = jsonMapper;
    }

    @Around("@annotation(idempotent)")
    public Object handleIdempotency(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        return Optional.ofNullable(request.getHeader(HEADER))
                .filter(key -> !key.isBlank())
                .map(key -> processWithIdempotency(joinPoint, idempotent, key))
                .orElseGet(() -> proceed(joinPoint));
    }

    private Object processWithIdempotency(ProceedingJoinPoint joinPoint, Idempotent idempotent, String key) {
        var fullKey = idempotent.namespace() + ":" + key;
        var ttl = Duration.ofSeconds(idempotent.ttlSeconds());

        var cached = idempotencyPort.getResponse(fullKey);
        if (cached.isPresent()) {
            log.debug("Idempotency hit: {}", fullKey);
            return deserialize(cached.get(), joinPoint);
        }

        if (!idempotencyPort.claim(fullKey, ttl)) {
            throw new IdempotencyConflictException(
                    "Request with idempotency key '" + key + "' is currently being processed");
        }

        var result = proceed(joinPoint);
        idempotencyPort.storeResponse(fullKey, serialize(result), ttl);
        log.debug("Idempotency stored: {}", fullKey);
        return result;
    }

    private String serialize(Object result) {
        try {
            var body = result instanceof ResponseEntity<?> re ? re.getBody() : result;
            return jsonMapper.writeValueAsString(body);
        } catch (Exception e) {
            log.warn("Idempotency serialization failed", e);
            return "{}";
        }
    }

    private Object deserialize(String json, ProceedingJoinPoint joinPoint) {
        try {
            var returnType = resolveBodyType(joinPoint);
            var body = jsonMapper.readValue(json, returnType);

            var methodReturnType = ((MethodSignature) joinPoint.getSignature()).getReturnType();
            if (ResponseEntity.class.isAssignableFrom(methodReturnType)) {
                return ResponseEntity.ok(body);
            }
            return body;
        } catch (Exception e) {
            log.warn("Idempotency deserialization failed — proceeding normally", e);
            return proceed(joinPoint);
        }
    }

    private Class<?> resolveBodyType(ProceedingJoinPoint joinPoint) {
        var method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        var returnType = method.getReturnType();

        if (ResponseEntity.class.isAssignableFrom(returnType)) {
            var genericType = method.getGenericReturnType();
            if (genericType instanceof java.lang.reflect.ParameterizedType pt) {
                var typeArg = pt.getActualTypeArguments()[0];
                if (typeArg instanceof Class<?> clazz) {
                    return clazz;
                }
            }
        }
        return returnType;
    }

    private Object proceed(ProceedingJoinPoint joinPoint) {
        try {
            return joinPoint.proceed();
        } catch (Throwable e) {
            throw e instanceof RuntimeException re ? re : new RuntimeException(e);
        }
    }
}
