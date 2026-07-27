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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemoveRequestHeaderIfNotCrossSiteGatewayFilterFactoryTest {

    private static final String ORIGIN = "https://trusted.example.com";

    private final RemoveRequestHeaderIfNotCrossSiteGatewayFilterFactory factory =
        new RemoveRequestHeaderIfNotCrossSiteGatewayFilterFactory();

    private ServerWebExchange runFilter(MockServerHttpRequest request) {
        var config = new RemoveRequestHeaderIfNotCrossSiteGatewayFilterFactory.Config();
        config.setName(HttpHeaders.ORIGIN);
        GatewayFilter filter = factory.apply(config);

        ReflectionTestUtils.setField(factory, "gatewayCorsEnabled", true);
        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();
        GatewayFilterChain chain = ex -> {
            captured.set(ex);
            return Mono.empty();
        };
        filter.filter(MockServerWebExchange.from(request), chain).block();
        return captured.get();
    }

    @Test
    void givenCrossSiteRequest_thenConfiguredHeaderPreserved() {
        ReflectionTestUtils.setField(factory, "preserveOrigin", true);
        MockServerHttpRequest request = MockServerHttpRequest.post("/service/api/v1/foo")
            .header(HttpHeaders.ORIGIN, ORIGIN)
            .header(RemoveRequestHeaderIfNotCrossSiteGatewayFilterFactory.SEC_FETCH_SITE_HEADER,
                RemoveRequestHeaderIfNotCrossSiteGatewayFilterFactory.CROSS_SITE)
            .build();

        ServerWebExchange result = runFilter(request);

        assertTrue(result.getRequest().getHeaders().containsKey(HttpHeaders.ORIGIN));
    }

    @ParameterizedTest
    @ValueSource(strings = {"same-origin", "same-site", "none"})
    void givenNonCrossSiteRequest_thenConfiguredHeaderRemoved(String secFetchSite) {
        MockServerHttpRequest request = MockServerHttpRequest.post("/service/api/v1/foo")
            .header(HttpHeaders.ORIGIN, ORIGIN)
            .header(RemoveRequestHeaderIfNotCrossSiteGatewayFilterFactory.SEC_FETCH_SITE_HEADER, secFetchSite)
            .build();

        ServerWebExchange result = runFilter(request);

        assertFalse(result.getRequest().getHeaders().containsKey(HttpHeaders.ORIGIN));
    }

    @Test
    void givenNoSecFetchSiteHeader_thenConfiguredHeaderRemoved() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/service/api/v1/foo")
            .header(HttpHeaders.ORIGIN, ORIGIN)
            .build();

        ServerWebExchange result = runFilter(request);

        assertFalse(result.getRequest().getHeaders().containsKey(HttpHeaders.ORIGIN));
    }

}
