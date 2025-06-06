/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.handler;

import com.netflix.discovery.shared.Applications;
import com.netflix.eureka.registry.PeerAwareInstanceRegistryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import org.zowe.apiml.product.constants.CoreService;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import org.zowe.apiml.util.HttpUtils;
import org.zowe.apiml.zaas.security.service.AuthenticationService;
import org.zowe.apiml.zaas.security.service.TokenCreationService;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class SuccessRefreshHandlerTest {

    private AuthenticationService authenticationService;
    private HttpUtils httpUtils;
    private TokenCreationService tokenCreationService;
    private PeerAwareInstanceRegistryImpl peerAwareInstanceRegistry;
    private SuccessRefreshHandler handler;

    @BeforeEach
    void setUp() {
        authenticationService = mock(AuthenticationService.class);
        httpUtils = mock(HttpUtils.class);
        tokenCreationService = mock(TokenCreationService.class);
        peerAwareInstanceRegistry = mock(PeerAwareInstanceRegistryImpl.class);
        handler = new SuccessRefreshHandler(authenticationService, httpUtils, tokenCreationService, peerAwareInstanceRegistry);
    }

    @Test
    void onAuthenticationSuccess_withTokenAuthentication_shouldInvalidateAndSetCookie() {
        String jwtToken = "jwt";
        String credentials = "credentials";
        String principal = "user";

        TokenAuthentication authentication = new TokenAuthentication(principal, credentials, TokenAuthentication.Type.JWT);

        ServerWebExchange exchange = mock(ServerWebExchange.class);
        MockServerHttpResponse response = new MockServerHttpResponse();
        when(exchange.getResponse()).thenReturn(response);

        WebFilterChain chain = mock(WebFilterChain.class);

        WebFilterExchange webFilterExchange = new WebFilterExchange(exchange, chain);

        Applications applications = mock(Applications.class);
        when(peerAwareInstanceRegistry.getApplications()).thenReturn(applications);
        when(applications.getRegisteredApplications(CoreService.GATEWAY.getServiceId())).thenReturn(null);

        when(tokenCreationService.createJwtTokenWithoutCredentials(principal)).thenReturn(jwtToken);

        ResponseCookie cookie = ResponseCookie.from("apimlAuthenticationToken", jwtToken).build();
        when(httpUtils.createResponseCookie(jwtToken)).thenReturn(cookie);

        Mono<Void> result = handler.onAuthenticationSuccess(webFilterExchange, authentication);

        StepVerifier.create(result).verifyComplete();

        verify(authenticationService).invalidateJwtTokenGateway(eq(credentials), eq(true), isNull());
        verify(tokenCreationService).createJwtTokenWithoutCredentials(principal);
        verify(httpUtils).createResponseCookie(jwtToken);
        assertEquals("jwt", Objects.requireNonNull(response.getCookies().getFirst("apimlAuthenticationToken")).getValue());

    }

    @Test
    void onAuthenticationSuccess_withOtherAuthentication_shouldDelegateToChain() {
        Authentication authentication = mock(Authentication.class);

        ServerWebExchange exchange = mock(ServerWebExchange.class);
        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        WebFilterExchange webFilterExchange = new WebFilterExchange(exchange, chain);

        Mono<Void> result = handler.onAuthenticationSuccess(webFilterExchange, authentication);

        StepVerifier.create(result).verifyComplete();

        verify(chain).filter(exchange);
        verifyNoInteractions(authenticationService, httpUtils, tokenCreationService);
    }
}
