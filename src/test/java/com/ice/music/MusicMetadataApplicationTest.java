package com.ice.music;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Smoke test: proves the full Spring context loads against
 * real PostgreSQL and Redis via Testcontainers.
 * If this test passes, the wiring is correct.
 */
@SpringBootTest
@Import(TestcontainersConfig.class)
class MusicMetadataApplicationTest {

    @Test
    void contextLoads() {
        // The context starting without exception is the assertion.
    }
}
