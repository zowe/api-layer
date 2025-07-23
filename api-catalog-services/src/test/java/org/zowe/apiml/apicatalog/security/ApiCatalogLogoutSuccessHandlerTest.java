/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.web.server.WebFilterChain;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ApiCatalogLogoutSuccessHandlerTest {

    @Test
    void testOnLogoutSuccess() {
        var request = MockServerHttpRequest.get("/logout")
            .header(HttpHeaders.AUTHORIZATION, "Bearer token123")
            .build();
        var exchange = MockServerWebExchange.from(request);
        WebFilterChain mockChain = mock(WebFilterChain.class);
        var webFilterExchange = new WebFilterExchange(exchange, mockChain);

        AuthConfigurationProperties securityConfigurationProperties = new AuthConfigurationProperties();
        ApiCatalogLogoutSuccessHandler apiCatalogLogoutSuccessHandler = new ApiCatalogLogoutSuccessHandler(securityConfigurationProperties);

        StepVerifier.create(apiCatalogLogoutSuccessHandler.onLogoutSuccess(
                webFilterExchange,
                new TokenAuthentication("TEST_TOKEN_STRING")
            ))
        .verifyComplete();

        StepVerifier.create(exchange.getSession())
            .assertNext(session -> {
                assertFalse(session.isStarted());
            })
            .verifyComplete();

        assertEquals(HttpStatus.OK.value(), exchange.getResponse().getStatusCode().value());

        var cookie = exchange.getResponse().getCookies().getFirst(
            securityConfigurationProperties.getCookieProperties().getCookieName());
        assertNotNull(cookie);
        assertTrue(cookie.isSecure());
        assertTrue(cookie.isHttpOnly());
    }

}
