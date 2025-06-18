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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest.BaseBuilder;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler;
import org.springframework.web.server.WebFilterChain;
import org.zowe.apiml.security.common.error.AuthMethodNotSupportedException;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import org.zowe.apiml.security.common.token.TokenNotProvidedException;
import org.zowe.apiml.security.common.token.TokenNotValidException;
import org.zowe.apiml.security.common.token.X509AuthenticationToken;
import org.zowe.apiml.util.HttpUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueryWebFilterTest {

    @Mock private ServerAuthenticationFailureHandler failureHandler;
    @Mock private ReactiveAuthenticationManager authenticationService;
    @Mock private WebFilterChain chain;
    private HttpUtils httpUtils = new HttpUtils(null);

    private QueryWebFilter filter;

    private MockServerWebExchange exchange;
    private MockServerHttpRequest request;

    @BeforeEach
    void setUp() {
        this.filter = new QueryWebFilter(failureHandler, HttpMethod.GET, false, authenticationService, httpUtils);
    }

    @Nested
    class GivenFilter {

        @Nested
        class GivenQuery {

            @Test
            void whenMethodNotSupported_thenFailureHandler() {
                request = MockServerHttpRequest.post("/someresource").build();
                exchange = MockServerWebExchange.from(request);

                when(failureHandler.onAuthenticationFailure(any(), isA(AuthMethodNotSupportedException.class)))
                    .thenReturn(Mono.empty());

                StepVerifier.create(filter.filter(exchange, chain))
                    .expectComplete()
                    .verify();
            }

            @Nested
            class GivenCorrectMethod {

                private Authentication authentication;

                private Mono<Void> testMono;

                @BeforeEach
                void setUp() {
                    initRequest(r -> {});
                    initAuthentication(X509AuthenticationToken.class, false);
                }

                private void initRequest(Consumer<BaseBuilder<?>> modifier) {
                    var r = MockServerHttpRequest.get("/someresource");
                    modifier.accept(r);
                    request = r.build();
                    exchange = MockServerWebExchange.from(request);
                }

                private void initAuthentication(Class<? extends Authentication> clasz, boolean certProtected) {
                    authentication = mock(clasz);
                    var securityContext = new SecurityContextImpl(authentication);
                    Mono<SecurityContext> contextMono = Mono.just(securityContext);

                    filter = new QueryWebFilter(failureHandler, HttpMethod.GET, certProtected, authenticationService, httpUtils);
                    testMono = filter.filter(exchange, chain)
                        .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(contextMono));
                }

                @Test
                void whenNoTokenProvided_thenError() {
                    initAuthentication(X509AuthenticationToken.class, true);
                    when(authentication.isAuthenticated()).thenReturn(true);

                    when(failureHandler.onAuthenticationFailure(any(), isA(TokenNotProvidedException.class)))
                        .thenReturn(Mono.empty());

                    StepVerifier.create(testMono)
                        .expectComplete()
                        .verify();
                }

                @Test
                void whenTokenIsInvalid_thenError() {
                    initRequest(r -> r.cookie(new HttpCookie("apimlAuthenticationToken", "expiredToken")));
                    initAuthentication(X509AuthenticationToken.class, true);
                    when(authentication.isAuthenticated()).thenReturn(true);

                    var tokenAuthentication = mock(Authentication.class);
                    when(authenticationService.authenticate(argThat(token -> token instanceof TokenAuthentication t && t.getType().equals(TokenAuthentication.Type.JWT) && t.getCredentials().equals("expiredToken"))))
                        .thenReturn(Mono.just(tokenAuthentication));

                    when(tokenAuthentication.isAuthenticated()).thenReturn(false);

                    when(failureHandler.onAuthenticationFailure(any(), isA(TokenNotValidException.class)))
                        .thenReturn(Mono.empty());

                    StepVerifier.create(testMono)
                        .expectComplete()
                        .verify();
                }

                @Test
                void whenProtectedByCert_UseCurrentAuthentication() {
                    initRequest(r -> r.cookie(new HttpCookie("apimlAuthenticationToken", "validToken")));
                    initAuthentication(X509AuthenticationToken.class, true);

                    when(authentication.isAuthenticated()).thenReturn(true);

                    when(authenticationService.authenticate(argThat(token -> token instanceof TokenAuthentication t && t.getType().equals(TokenAuthentication.Type.JWT) && t.getCredentials().equals("validToken"))))
                        .thenReturn(Mono.just(authentication));

                    var mockMono = mock(Mono.class);

                    when(chain.filter(exchange)).thenReturn(mockMono);
                    when(mockMono.contextWrite(any(Context.class))).thenReturn(Mono.empty());

                    StepVerifier.create(testMono)
                        .expectComplete()
                        .verify();
                }

            }

        }

    }

}
