package com.ice.music.adapter.out.persistence;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables Spring Data JPA Auditing.
 *
 * This activates @CreatedDate and @LastModifiedDate on entities
 * annotated with @EntityListeners(AuditingEntityListener.class).
 * Zero-overhead housekeeping - no shadow tables, no triggers.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
