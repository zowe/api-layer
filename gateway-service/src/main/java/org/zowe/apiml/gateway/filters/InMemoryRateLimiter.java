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

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryRateLimiter implements RateLimiter<InMemoryRateLimiter.Config> {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();
    @Value("${apiml.gateway.rateLimiterCapacity:3}")
    int capacity;
    @Value("${apiml.gateway.rateLimiterTokens:3}")
    int tokens;
    @Value("${apiml.gateway.rateLimiterRefillDuration:1}")
    Long refillDuration;

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
    public Map<String, Config> getConfig() {
        Config defaultConfig = new Config();
        defaultConfig.setCapacity(capacity);
        defaultConfig.setTokens(tokens);
        defaultConfig.setRefillDuration(refillDuration);

        Map<String, Config> configMap = new ConcurrentHashMap<>();
        configMap.put("default", defaultConfig);
        return configMap;
    }

    @Override
    public Class<Config> getConfigClass() {
        return Config.class;
    }

    @Override
    public Config newConfig() {
        Config config = new Config();
        config.setCapacity(capacity);
        config.setTokens(tokens);
        config.setRefillDuration(refillDuration);
        return config;
    }

    @Setter
    @Getter
    public static class Config {
        private int capacity;
        private int tokens;
        private Long refillDuration;
    }
}
