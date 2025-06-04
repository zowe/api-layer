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
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import org.zowe.apiml.gateway.filters.security.AuthExceptionHandlerReactive;
import org.zowe.apiml.handler.LocalTokenProvider;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties.CookieProperties;
import org.zowe.apiml.security.common.token.QueryResponse;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.function.Function;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TokenAuthenticationFilterTest {

    @Mock private LocalTokenProvider tokenProvider;
    @Mock private AuthConfigurationProperties authConfigurationProperties;
    @Mock private AuthExceptionHandlerReactive authExceptionHandlerReactive;
    @Mock private WebFilterChain chain;

    private MockServerWebExchange exchange;
    private MockServerHttpRequest request;

    private TokenAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new TokenAuthenticationFilter(tokenProvider, authConfigurationProperties, authExceptionHandlerReactive);
        var cookieProperties = new CookieProperties();
        cookieProperties.setCookieName("apimlAuthenticationToken");
        lenient().when(authConfigurationProperties.getCookieProperties()).thenReturn(cookieProperties);
        request = null;
        exchange = null;
    }

    @Nested
    class GivenFilter {

        @Nested
        class GivenRequestWithoutAuth {

            @Test
            void thenContinueChain() {
                request = MockServerHttpRequest.get("/someresource").build();
                exchange = MockServerWebExchange.builder(request).build();

                when(chain.filter(exchange)).thenReturn(Mono.empty());

                StepVerifier.create(filter.filter(exchange, chain))
                    .expectComplete()
                    .verify();

                verifyNoInteractions(authExceptionHandlerReactive);
            }

        }

        @Nested
        class GivenRequestWithBearer {

            @Test
            @SuppressWarnings("unchecked")
            void whenValidToken_createAuthentication() {
                request = MockServerHttpRequest.get("/someresource")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer validToken")
                    .build();
                exchange = MockServerWebExchange.builder(request).build();
                var queryResponse = new QueryResponse();
                queryResponse.setUserId("validuser");
                Mono<Void> mockMono = mock(Mono.class);

                when(tokenProvider.validateToken("validToken")).thenReturn(Mono.just(queryResponse));
                when(chain.filter(exchange)).thenReturn(mockMono);
                when(mockMono.contextWrite(any(Function.class))).thenReturn(Mono.empty());

                StepVerifier.create(filter.filter(exchange, chain))
                    .expectComplete()
                    .verify();

                verifyNoInteractions(authExceptionHandlerReactive);
            }

            @Test
            void whenValidTokenLowerCase_thenContinueChain() {
                request = MockServerHttpRequest.get("/someresource")
                    .header(HttpHeaders.AUTHORIZATION, "bearer validToken")
                    .build();
                exchange = MockServerWebExchange.builder(request).build();

                when(chain.filter(exchange)).thenReturn(Mono.empty());

                StepVerifier.create(filter.filter(exchange, chain))
                    .expectComplete()
                    .verify();

                verifyNoInteractions(authExceptionHandlerReactive);
            }

            @Test
            void whenInvalidToken_thenExceptionHandler() {
                request = MockServerHttpRequest.get("/someresource")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer invalidToken")
                    .build();
                exchange = MockServerWebExchange.builder(request).build();

                when(tokenProvider.validateToken("invalidToken")).thenReturn(Mono.just(new QueryResponse()));
                when(authExceptionHandlerReactive.handleTokenNotValid(exchange)).thenReturn(Mono.empty());

                StepVerifier.create(filter.filter(exchange, chain))
                    .expectComplete()
                    .verify();

                verifyNoInteractions(chain);
            }

        }

        @Nested
        class GivenRequestWithAuthCookie {

            @Test
            @SuppressWarnings("unchecked")
            void whenValidToken_createAuthentication() {
                request = MockServerHttpRequest.get("/someresource")
                    .cookie(new HttpCookie("apimlAuthenticationToken", "validTokenCookie"))
                    .build();
                exchange = MockServerWebExchange.builder(request).build();
                var queryResponse = new QueryResponse();
                queryResponse.setUserId("validuser");
                Mono<Void> mockMono = mock(Mono.class);

                when(tokenProvider.validateToken("validTokenCookie")).thenReturn(Mono.just(queryResponse));
                when(chain.filter(exchange)).thenReturn(mockMono);
                when(mockMono.contextWrite(any(Function.class))).thenReturn(Mono.empty());

                StepVerifier.create(filter.filter(exchange, chain))
                    .expectComplete()
                    .verify();

                verifyNoInteractions(authExceptionHandlerReactive);
            }

        }

        @Nested
        class GivenRequestWithBothTokens {

            @Test
            @SuppressWarnings("unchecked")
            void thenPreferBearer() {
                request = MockServerHttpRequest.get("/someresource")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer validTokenBearer")
                    .cookie(new HttpCookie("apimlAuthenticationToken", "validTokenCookie"))
                    .build();
                exchange = MockServerWebExchange.builder(request).build();

                var queryResponse = new QueryResponse();
                queryResponse.setUserId("validuser");
                Mono<Void> mockMono = mock(Mono.class);

                when(tokenProvider.validateToken("validTokenBearer")).thenReturn(Mono.just(queryResponse));
                when(chain.filter(exchange)).thenReturn(mockMono);
                when(mockMono.contextWrite(any(Function.class))).thenReturn(Mono.empty());

                StepVerifier.create(filter.filter(exchange, chain))
                    .expectComplete()
                    .verify();

                verifyNoInteractions(authExceptionHandlerReactive);
            }

        }

    }

}
