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
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class KeyResolverTest {

    private KeyResolver keyResolver;
    private ServerWebExchange exchange;

    @BeforeEach
    public void setUp() {
        keyResolver = new KeyResolver();
        exchange = mock(ServerWebExchange.class);
    }

    @Test
    public void resolve_shouldReturnCookieValue_whenCookieIsPresent() {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        HttpCookie cookie = new HttpCookie("apimlAuthenticationToken", "testToken");
        when(exchange.getRequest()).thenReturn(request);
        var cookies = new LinkedMultiValueMap<String, HttpCookie>();
        cookies.add("apimlAuthenticationToken", cookie);
        when(request.getCookies()).thenReturn(cookies);

        Mono<String> result = keyResolver.resolve(exchange);

        assertEquals("testToken", result.block());
    }

    @Test
    public void resolve_shouldReturnNull_whenCookieIsNotPresent() {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        when(exchange.getRequest()).thenReturn(request);
        when(request.getCookies()).thenReturn(new LinkedMultiValueMap<>());

        Mono<String> result = keyResolver.resolve(exchange);

        assertNull(result.block());
    }
}

