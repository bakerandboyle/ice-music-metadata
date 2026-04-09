package com.ice.music.adapter.in.web;

import com.ice.music.port.out.IdempotencyPort;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyAspectTest {

    @Mock
    private IdempotencyPort idempotencyPort;

    @Mock
    private HttpServletRequest request;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private IdempotencyAspect aspect() {
        return new IdempotencyAspect(idempotencyPort, request, jsonMapper);
    }

    private Idempotent idempotent() {
        return new Idempotent() {
            @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return Idempotent.class; }
            @Override public String namespace() { return "test"; }
            @Override public long ttlSeconds() { return 3600; }
        };
    }

    @Test
    void noHeader_proceedsNormally() throws Throwable {
        when(request.getHeader("X-Idempotency-Key")).thenReturn(null);
        when(joinPoint.proceed()).thenReturn("result");

        var result = aspect().handleIdempotency(joinPoint, idempotent());

        assertThat(result).isEqualTo("result");
        verify(idempotencyPort, never()).getResponse(anyString());
    }

    @Test
    void cachedResponse_returnsWithoutProceeding() throws Throwable {
        when(request.getHeader("X-Idempotency-Key")).thenReturn("key-1");
        when(idempotencyPort.getResponse("test:key-1")).thenReturn(Optional.of("\"Queen\""));

        when(joinPoint.getSignature()).thenReturn(methodSignature);

        // Simulate a method returning String
        Method method = getClass().getDeclaredMethod("dummyMethod");
        when(methodSignature.getMethod()).thenReturn(method);
        when(methodSignature.getReturnType()).thenReturn(String.class);

        aspect().handleIdempotency(joinPoint, idempotent());

        verify(joinPoint, never()).proceed();
        verify(idempotencyPort, never()).claim(anyString(), any());
    }

    @Test
    void newKey_claimsAndProceeds() throws Throwable {
        when(request.getHeader("X-Idempotency-Key")).thenReturn("new-key");
        when(idempotencyPort.getResponse("test:new-key")).thenReturn(Optional.empty());
        when(idempotencyPort.claim("test:new-key", Duration.ofSeconds(3600))).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("result");

        var result = aspect().handleIdempotency(joinPoint, idempotent());

        assertThat(result).isEqualTo("result");
        verify(idempotencyPort).storeResponse(eq("test:new-key"), anyString(), eq(Duration.ofSeconds(3600)));
    }

    @Test
    void claimFails_throwsConflict() throws Throwable {
        when(request.getHeader("X-Idempotency-Key")).thenReturn("contested-key");
        when(idempotencyPort.getResponse("test:contested-key")).thenReturn(Optional.empty());
        when(idempotencyPort.claim("test:contested-key", Duration.ofSeconds(3600))).thenReturn(false);

        assertThatThrownBy(() -> aspect().handleIdempotency(joinPoint, idempotent()))
                .isInstanceOf(IdempotencyConflictException.class);

        verify(joinPoint, never()).proceed();
    }

    @Test
    void blankHeader_proceedsNormally() throws Throwable {
        when(request.getHeader("X-Idempotency-Key")).thenReturn("   ");
        when(joinPoint.proceed()).thenReturn("result");

        var result = aspect().handleIdempotency(joinPoint, idempotent());

        assertThat(result).isEqualTo("result");
        verify(idempotencyPort, never()).getResponse(anyString());
    }

    // Dummy method for reflection in deserialization test
    String dummyMethod() { return ""; }
}
