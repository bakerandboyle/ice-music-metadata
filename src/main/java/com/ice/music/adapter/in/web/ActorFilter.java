package com.ice.music.adapter.in.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Captures the X-Actor-Id header and binds it to a ScopedValue
 * for the duration of the request.
 *
 * ScopedValue (Java 25 LTS) is the virtual-thread-safe replacement
 * for ThreadLocal — zero proxy overhead, no carrier-thread leakage,
 * immutable within scope.
 *
 * Runs before all other filters (Ordered.HIGHEST_PRECEDENCE) so the
 * actor identity is available throughout the request lifecycle.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ActorFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Actor-Id";
    private static final String ANONYMOUS = "anonymous";

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        var actorId = Optional.ofNullable(request.getHeader(HEADER))
                .filter(id -> !id.isBlank())
                .orElse(ANONYMOUS);

        try {
            ScopedValue.where(ActorScopedValue.ACTOR_ID, actorId).call(() -> {
                filterChain.doFilter(request, response);
                return null;
            });
        } catch (ServletException | IOException e) {
            throw e;
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
