/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.filters;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class InMemoryRateLimiterTest {

    private InMemoryRateLimiter rateLimiter;
    String userId = "testUser";
    String routeId = "testRoute";

    @BeforeEach
    void setUp() {
        rateLimiter = new InMemoryRateLimiter();
        rateLimiter.capacity = 3;
        rateLimiter.tokens = 3;
        rateLimiter.refillDuration = 1;
    }

    @Test
    void isAllowed_shouldReturnTrue_whenTokensAvailable() {
        rateLimiter.capacity = 1;

        var elapsed = StepVerifier.create(rateLimiter.isAllowed(routeId, userId))
            .assertNext(response -> assertTrue(response.isAllowed()))
            .verifyComplete();
        assertEquals(0L, elapsed.toSeconds());
    }

    @Test
    void isAllowed_shouldReturnFalse_whenTokensExhausted() {
        for (int i = 0; i < rateLimiter.capacity; i++) {
            var count = i;

            var elapsed = StepVerifier.create(rateLimiter.isAllowed(routeId, userId))
                .assertNext(response -> assertTrue(response.isAllowed(), "Request " + (count + 1) + " should be allowed"))
                .verifyComplete();
            assertEquals(0L, elapsed.toSeconds());
        }
        // Last request should be denied
        var elapsed = StepVerifier.create(rateLimiter.isAllowed(routeId, userId))
            .assertNext(response -> assertFalse(response.isAllowed(), "Fourth request should not be allowed"))
            .verifyComplete();
        assertEquals(0L, elapsed.toSeconds());
    }

    @Test
    void testDifferentClientIdHasSeparateBucket() {
        var clientId1 = "client1";
        var clientId2 = "client2";

        // Allow first three requests for client1
        for (int i = 0; i < rateLimiter.capacity; i++) {
            var count = i;
            var elapsed = StepVerifier.create(rateLimiter.isAllowed(routeId, clientId1))
                .assertNext(response -> {
                    assertTrue(response.isAllowed(), "Request " + (count + 1) + " for client1 should be allowed");
                })
                .verifyComplete();
            assertEquals(0L, elapsed.toSeconds());
        }

        // Fourth request for client1 should be denied
        var elapsed = StepVerifier.create(rateLimiter.isAllowed(routeId, clientId1))
            .assertNext(response -> assertFalse(response.isAllowed(), "Fourth request for client1 should not be allowed"))
            .verifyComplete();
        assertEquals(0L, elapsed.toSeconds());

        // Allow first request for client2, it should be allowed since it's a separate bucket
        elapsed = StepVerifier.create(rateLimiter.isAllowed(routeId, clientId2))
            .assertNext(response -> assertTrue(response.isAllowed(), "First request for client2 should be allowed"))
            .verifyComplete();
        assertEquals(0L, elapsed.toSeconds());
    }

    @Test
    void testNewConfig() {
        InMemoryRateLimiter.Config config = rateLimiter.newConfig();

        assertNotNull(config, "Config should not be null");
        assertEquals(rateLimiter.capacity, config.getCapacity(), "Config capacity should match the rate limiter capacity");
        assertEquals(rateLimiter.tokens, config.getTokens(), "Config tokens should match the rate limiter tokens");
        assertEquals(rateLimiter.refillDuration, config.getRefillDuration(), "Config refill duration should match the rate limiter refill duration");
    }

    @Test
    void setNonNullParametersTest() {
        Integer newCapacity = 20;
        Integer newTokens = 20;
        Integer newRefillDuration = 2;
        rateLimiter.setParameters(newCapacity, newTokens, newRefillDuration);
        assertEquals(newCapacity, rateLimiter.capacity);
        assertEquals(newTokens, rateLimiter.tokens);
        assertEquals(newRefillDuration, rateLimiter.refillDuration);
    }

    @Test
    void setParametersWithNullValuesTest() {
        Integer newCapacity = 30;
        rateLimiter.setParameters(newCapacity, 0, 0);
        assertEquals(newCapacity, rateLimiter.capacity);
    }

}
