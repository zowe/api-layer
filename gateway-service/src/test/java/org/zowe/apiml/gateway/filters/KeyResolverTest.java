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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeyResolverTest {

    @Mock private ServerHttpRequest request;

    private KeyResolver keyResolver;
    private ServerWebExchange exchange;

    @BeforeEach
    void setUp() {
        keyResolver = new KeyResolver();
        exchange = mock(ServerWebExchange.class);
        ReflectionTestUtils.setField(keyResolver, "cookieName", "apimlAuthenticationToken");
    }

    @Test
    void resolve_shouldReturnCookieValue_whenCookieIsPresent() {
        HttpCookie cookie = new HttpCookie("apimlAuthenticationToken", "testToken");
        when(exchange.getRequest()).thenReturn(request);
        var cookies = new LinkedMultiValueMap<String, HttpCookie>();
        cookies.add("apimlAuthenticationToken", cookie);
        when(request.getCookies()).thenReturn(cookies);

        StepVerifier.create(keyResolver.resolve(exchange))
            .assertNext(result -> assertEquals("testToken", result))
            .verifyComplete();
    }

    @Test
    void resolve_shouldReturnNull_whenCookieIsNotPresent() {
        when(exchange.getRequest()).thenReturn(request);
        when(request.getCookies()).thenReturn(new LinkedMultiValueMap<>());

        StepVerifier.create(keyResolver.resolve(exchange))
            .assertNext(result -> assertEquals("", result))
            .verifyComplete();
    }
}

