/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.filter;

import com.netflix.discovery.shared.Applications;
import com.netflix.eureka.registry.PeerAwareInstanceRegistryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.web.server.WebFilterChain;
import org.zowe.apiml.handler.FailedAuthenticationWebHandler;
import org.zowe.apiml.security.common.token.TokenFormatNotValidException;
import org.zowe.apiml.security.common.token.TokenNotValidException;
import org.zowe.apiml.util.HttpUtils;
import org.zowe.apiml.zaas.security.service.AuthenticationService;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogoutHandlerTest {

    @Mock private AuthenticationService authenticationService;
    @Mock private FailedAuthenticationWebHandler failureHandler;
    @Mock private PeerAwareInstanceRegistryImpl registry;
    private HttpUtils httpUtils = new HttpUtils(null);

    private LogoutHandler logoutHandler;

    @BeforeEach
    void setUp() {
        logoutHandler = new LogoutHandler(authenticationService, failureHandler, registry, httpUtils);
    }

    @Test
    void shouldCallFailureHandlerWhenTokenAlreadyInvalidated() {
        var request = MockServerHttpRequest.get("/logout")
            .header(HttpHeaders.AUTHORIZATION, "Bearer token123")
            .build();
        var exchange = MockServerWebExchange.from(request);
        WebFilterChain mockChain = mock(WebFilterChain.class);
        var webFilterExchange = new WebFilterExchange(exchange, mockChain);

        when(authenticationService.isInvalidated("token123")).thenReturn(true);
        when(failureHandler.onAuthenticationFailure(eq(webFilterExchange), any(TokenNotValidException.class)))
            .thenReturn(Mono.empty());

        StepVerifier.create(logoutHandler.logout(webFilterExchange, mock(Authentication.class)))
            .verifyComplete();

        verify(failureHandler).onAuthenticationFailure(eq(webFilterExchange), any(TokenNotValidException.class));
    }

    @Test
    void shouldCallFailureHandlerWhenTokenFormatInvalid() {
        var request = MockServerHttpRequest.get("/logout")
            .header(HttpHeaders.AUTHORIZATION, "Bearer token123")
            .build();
        var exchange = MockServerWebExchange.from(request);
        WebFilterChain mockChain = mock(WebFilterChain.class);
        var webFilterExchange = new WebFilterExchange(exchange, mockChain);

        when(authenticationService.isInvalidated("token123")).thenReturn(false);
        when(registry.getApplications()).thenThrow(new TokenFormatNotValidException("bad format"));
        when(failureHandler.onAuthenticationFailure(eq(webFilterExchange), any(TokenFormatNotValidException.class)))
            .thenReturn(Mono.empty());

        StepVerifier.create(logoutHandler.logout(webFilterExchange, mock(Authentication.class)))
            .verifyComplete();

        verify(failureHandler).onAuthenticationFailure(eq(webFilterExchange), any(TokenFormatNotValidException.class));
    }

    @Test
    void shouldCallFailureHandlerOnUnexpectedException() {
        var request = MockServerHttpRequest.get("/logout")
            .header(HttpHeaders.AUTHORIZATION, "Bearer token123")
            .build();
        var exchange = MockServerWebExchange.from(request);
        WebFilterChain mockChain = mock(WebFilterChain.class);
        var webFilterExchange = new WebFilterExchange(exchange, mockChain);

        when(authenticationService.isInvalidated("token123")).thenReturn(false);
        when(registry.getApplications()).thenThrow(new RuntimeException("unexpected error"));
        when(failureHandler.onAuthenticationFailure(eq(webFilterExchange), any(TokenNotValidException.class)))
            .thenReturn(Mono.empty());

        StepVerifier.create(logoutHandler.logout(webFilterExchange, mock(Authentication.class)))
            .verifyComplete();

        verify(failureHandler).onAuthenticationFailure(eq(webFilterExchange), any(TokenNotValidException.class));
    }

    @Test
    void shouldInvalidateValidTokenSuccessfully() {
        var request = MockServerHttpRequest.get("/logout")
            .header(HttpHeaders.AUTHORIZATION, "Bearer token123")
            .build();
        var exchange = MockServerWebExchange.from(request);
        WebFilterChain mockChain = mock(WebFilterChain.class);
        var webFilterExchange = new WebFilterExchange(exchange, mockChain);

        when(authenticationService.isInvalidated("token123")).thenReturn(false);
        Applications mockApplications = mock(Applications.class);
        when(registry.getApplications()).thenReturn(mockApplications);

        StepVerifier.create(logoutHandler.logout(webFilterExchange, mock(Authentication.class)))
            .verifyComplete();

        verify(authenticationService).invalidateJwtTokenGateway(eq("token123"), eq(true), any());
    }

}
