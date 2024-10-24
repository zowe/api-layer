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
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class InMemoryRateLimiterFilterFactoryTest {

    private InMemoryRateLimiter rateLimiter;
    private KeyResolver keyResolver;
    private InMemoryRateLimiterFilterFactory filterFactory;
    private ServerWebExchange exchange;
    private GatewayFilterChain chain;
    private String serviceId;
    private MockServerHttpRequest request;
    private InMemoryRateLimiterFilterFactory.Config config;

    @BeforeEach
    public void setUp() {
        serviceId = "testService";
        rateLimiter = mock(InMemoryRateLimiter.class);
        keyResolver = mock(KeyResolver.class);
        filterFactory = new InMemoryRateLimiterFilterFactory(rateLimiter, keyResolver);
        filterFactory.serviceIds = List.of(serviceId);
        request = MockServerHttpRequest.get("/" + serviceId).build();
        exchange = MockServerWebExchange.from(request);
        chain = mock(GatewayFilterChain.class);
        config = mock(InMemoryRateLimiterFilterFactory.Config.class);
        when(config.getRouteId()).thenReturn("testRoute");
    }

    @Test
    public void apply_shouldAllowRequest_whenTokensAreAvailable() {
        when(keyResolver.resolve(exchange)).thenReturn(Mono.just("testKey"));
        when(rateLimiter.isAllowed(anyString(), anyString())).thenReturn(Mono.just(new RateLimiter.Response(true, Map.of())));
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        StepVerifier.create(filterFactory.apply(config).filter(exchange, chain))
            .expectComplete()
            .verify();
        verify(chain, times(1)).filter(exchange);
    }

    @Test
    public void apply_shouldReturn429_whenTokensAreExhausted() {
        when(keyResolver.resolve(exchange)).thenReturn(Mono.just("testKey"));
        when(rateLimiter.isAllowed(anyString(), anyString())).thenReturn(Mono.just(new InMemoryRateLimiter.Response(false, Map.of())));

        StepVerifier.create(filterFactory.apply(config).filter(exchange, chain))
            .expectComplete()
            .verify();
        ServerHttpResponse response = exchange.getResponse();
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
    }

    @Test
    public void apply_shouldAllowRequest_whenKeyIsNull() {
        when(keyResolver.resolve(exchange)).thenReturn(Mono.just(""));
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        StepVerifier.create(filterFactory.apply(config).filter(exchange, chain))
            .expectComplete()
            .verify();
        verify(chain, times(1)).filter(exchange);
    }

    @Test
    public void apply_shouldAllowRequest_whenServiceIdDoesNotMatch() {
        String nonMatchingId = "nonMatchingId";
        when(keyResolver.resolve(exchange)).thenReturn(Mono.just("testKey"));
        request = MockServerHttpRequest.get("/" + nonMatchingId).build();
        exchange = MockServerWebExchange.from(request);
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        StepVerifier.create(filterFactory.apply(config).filter(exchange, chain))
            .expectComplete()
            .verify();
        verify(chain, times(1)).filter(exchange);
    }

}

