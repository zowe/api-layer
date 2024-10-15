package org.zowe.apiml.gateway.filters;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import reactor.core.publisher.Mono;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InMemoryRateLimiterTest {

    private InMemoryRateLimiter rateLimiter;
    String userId = "testUser";
    String routeId = "testRoute";

    @BeforeEach
    public void setUp() {
        rateLimiter = new InMemoryRateLimiter();
        rateLimiter.capacity = 3;
        rateLimiter.tokens = 3;
        rateLimiter.refillDuration = 1L;
    }

    @Test
    public void isAllowed_shouldReturnTrue_whenTokensAvailable() {
        rateLimiter.capacity = 1;

        Mono<RateLimiter.Response> response = rateLimiter.isAllowed(routeId, userId);

        assertTrue(Objects.requireNonNull(response.block()).isAllowed());
    }

    @Test
    public void isAllowed_shouldReturnFalse_whenTokensExhausted() {
        for (int i = 0; i < rateLimiter.capacity; i++) {
            Mono<InMemoryRateLimiter.Response> responseMono = rateLimiter.isAllowed(routeId, userId);
            InMemoryRateLimiter.Response response = responseMono.block();
            assertTrue(response.isAllowed(), "Request " + (i + 1) + " should be allowed");
        }
        // Last request should be denied
        Mono<InMemoryRateLimiter.Response> responseMono = rateLimiter.isAllowed(routeId, userId);
        InMemoryRateLimiter.Response response = responseMono.block();
        assertFalse(response.isAllowed(), "Fourth request should not be allowed");
    }

    @Test
    public void testDifferentClientIdHasSeparateBucket() {
        String clientId1 = "client1";
        String clientId2 = "client2";

        // Allow first three requests for client1
        for (int i = 0; i < rateLimiter.capacity; i++) {
            Mono<InMemoryRateLimiter.Response> responseMono = rateLimiter.isAllowed(routeId, clientId1);
            InMemoryRateLimiter.Response response = responseMono.block();
            assertTrue(response.isAllowed(), "Request " + (i + 1) + " for client1 should be allowed");
        }

        // Fourth request for client1 should be denied
        Mono<InMemoryRateLimiter.Response> responseMono = rateLimiter.isAllowed(routeId, clientId1);
        InMemoryRateLimiter.Response response = responseMono.block();
        assertFalse(response.isAllowed(), "Fourth request for client1 should not be allowed");

        // Allow first request for client2, it should be allowed since it's a separate bucket
        Mono<InMemoryRateLimiter.Response> responseMono2 = rateLimiter.isAllowed(routeId, clientId2);
        InMemoryRateLimiter.Response response2 = responseMono2.block();
        assertTrue(response2.isAllowed(), "First request for client2 should be allowed");
    }
}
