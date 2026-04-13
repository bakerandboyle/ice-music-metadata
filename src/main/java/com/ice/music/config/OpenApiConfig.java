package com.ice.music.config;

import com.ice.music.adapter.in.web.Idempotent;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3.1 configuration.
 *
 * Produces the /v3/api-docs spec and /swagger-ui.html interactive UI.
 * Global headers (X-ICE-Version, X-Actor-Id) are injected into every operation.
 * X-Idempotency-Key is injected only for methods annotated with @Idempotent.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI iceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("ICE Music Metadata Service")
                        .version("2026-04-09")
                        .description("""
                                Production-ready music metadata service for a streaming platform. \
                                Hexagonal architecture, Java 25 virtual threads, Redis-backed caching \
                                with pub/sub stampede protection, ISRC (ISO 3901) natural keys, \
                                typed audit event emission, three-tier idempotency.""")
                        .contact(new Contact()
                                .name("Steve Baker — Baker & Boyle Research")));
    }

    /**
     * Adds platform headers to every operation.
     *
     * <ul>
     *   <li>X-ICE-Version — contract version (date scheme), optional</li>
     *   <li>X-Actor-Id — audit identity via ScopedValue, optional</li>
     *   <li>X-Idempotency-Key — deduplication key, only on @Idempotent methods</li>
     * </ul>
     */
    @Bean
    public OperationCustomizer globalHeadersCustomizer() {
        return (operation, handlerMethod) -> {
            operation.addParametersItem(new HeaderParameter()
                    .name("X-ICE-Version")
                    .description("API contract version (date scheme). Omit for latest.")
                    .required(false)
                    .schema(new StringSchema().example("2026-04-09")));

            operation.addParametersItem(new HeaderParameter()
                    .name("X-Actor-Id")
                    .description("Identity of the caller. Propagated to audit events "
                            + "via ScopedValue. Defaults to 'anonymous'.")
                    .required(false)
                    .schema(new StringSchema().example("user-xyz")));

            if (handlerMethod.getMethodAnnotation(Idempotent.class) != null) {
                operation.addParametersItem(new HeaderParameter()
                        .name("X-Idempotency-Key")
                        .description("Client-generated unique key for request deduplication. "
                                + "Cached responses returned for duplicate keys within the TTL window.")
                        .required(false)
                        .schema(new StringSchema().example("550e8400-e29b-41d4-a716-446655440000")));
            }

            return operation;
        };
    }
}
