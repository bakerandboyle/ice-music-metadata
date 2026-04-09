package com.ice.music.adapter.out.cache;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Provides a Clock bean for time-dependent services.
 * Production: UTC system clock.
 * Tests: inject a fixed Clock for deterministic assertions.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
