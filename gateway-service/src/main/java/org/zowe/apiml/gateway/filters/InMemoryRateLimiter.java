package org.zowe.apiml.gateway.filters;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryRateLimiter implements RateLimiter<Object> {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();
    @Value("${apiml.gateway.rateLimiterCapacity:3}")
    private int capacity;

    @Value("${apiml.gateway.rateLimiterTokens:3}")
    private int tokens;
    @Value("${apiml.gateway.rateLimiterRefillDuration:1}")
    private Long refillDuration;

    @Override
    public Mono<Response> isAllowed(String routeId, String id) {
        Bucket bucket = cache.computeIfAbsent(id, this::newBucket);
        if (bucket.tryConsume(1)) {
            return Mono.just(new Response(true, getHeaders(bucket)));
        } else {
            return Mono.just(new Response(false, getHeaders(bucket)));
        }
    }

    private Bucket newBucket(String id) {
        Bandwidth limit = Bandwidth.builder().capacity(capacity).refillGreedy(tokens, Duration.ofMinutes(refillDuration)).build();
        return Bucket.builder().addLimit(limit).build();
    }

    private Map<String, String> getHeaders(Bucket bucket) {
        Map<String, String> headers = new ConcurrentHashMap<>();
        headers.put("X-RateLimit-Remaining", String.valueOf(bucket.getAvailableTokens()));
        return headers;
    }

    @Override
    public Map<String, Object> getConfig() {
        return Map.of();
    }

    @Override
    public Class<Object> getConfigClass() {
        return null;
    }

    @Override
    public Object newConfig() {
        return null;
    }
}
