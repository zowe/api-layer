/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.filters.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import org.zowe.apiml.gateway.service.TokenProvider;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties.CookieProperties;
import org.zowe.apiml.security.common.token.QueryResponse;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.function.Function;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.util.CollectionUtils.toMultiValueMap;

@ExtendWith(MockitoExtension.class)
class TokenAuthFilterTest {

    private static final String COOKIE_NAME = "apimlAuthenticationToken";

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private WebFilterChain chain;

    @Mock
    private ServerWebExchange serverWebExchange;

    @Mock
    private ServerHttpRequest httpRequest;

    @Mock
    private AuthConfigurationProperties authConfigurationProperties;

    private TokenAuthFilter tokenAuthFilter;

    @Nested
    class GivenTokenFilter {

        @BeforeEach
        void setUp() {
            when(serverWebExchange.getRequest()).thenReturn(httpRequest);

            tokenAuthFilter = new TokenAuthFilter(tokenProvider, authConfigurationProperties);
        }

        @Nested
        class WhenTokenIsInRequest {

            private void mockTokenInCookie() {
                var cookieProperties = mock(CookieProperties.class);
                when(authConfigurationProperties.getCookieProperties()).thenReturn(cookieProperties);
                when(cookieProperties.getCookieName()).thenReturn(COOKIE_NAME);
                when(httpRequest.getHeaders())
                    .thenReturn(new HttpHeaders(
                        toMultiValueMap(
                            singletonMap("Cookie", asList("apimlAuthenticationToken=token")))));
            }

            private void mockTokenInHeader() {
                when(httpRequest.getHeaders())
                    .thenReturn(new HttpHeaders(
                        toMultiValueMap(
                            singletonMap("Authorization", asList("Bearer token"))
                        )
                    ));
            }

            @Test
            void givenTokenIsInHeaderValid_thenCreateAuthentication() {
                mockTokenInHeader();

                QueryResponse response = new QueryResponse();
                response.setUserId("user");
                when(tokenProvider.validateToken("token")).thenReturn(Mono.just(response));
                Mono<Void> monoSpy = spy(Mono.empty());
                when(chain.filter(any())).thenReturn(monoSpy);

                StepVerifier.create(tokenAuthFilter.filter(serverWebExchange, chain))
                    .expectComplete()
                    .verify();

                verify(monoSpy, times(1)).contextWrite(any(Function.class));
            }

            @SuppressWarnings("unchecked")
            @Test
            void givenTokenIsInCookieValid_thenCreateAuthentication() {
                mockTokenInCookie();
                QueryResponse response = new QueryResponse();
                response.setUserId("user");
                when(tokenProvider.validateToken("token")).thenReturn(Mono.just(response));
                Mono<Void> monoSpy = spy(Mono.empty());
                when(chain.filter(any())).thenReturn(monoSpy);

                StepVerifier.create(tokenAuthFilter.filter(serverWebExchange, chain))
                    .expectComplete()
                    .verify();

                verify(monoSpy, times(1)).contextWrite(any(Function.class));
            }

            @Test
            void givenTokenIsInvalidError_thenStopChainAndReturnError() {
                mockTokenInCookie();

                when(tokenProvider.validateToken("token")).thenReturn(Mono.error(new RuntimeException("error in validation")));
                StepVerifier.create(tokenAuthFilter.filter(serverWebExchange, chain))
                    .expectErrorSatisfies(error -> assertEquals("error in validation", error.getMessage()))
                    .verify();

                verify(chain, never()).filter(serverWebExchange);
            }

            @Test
            void givenTokenIsInvalidEmpty_thenStopChainAndReturnEmpty() {
                mockTokenInCookie();
                when(tokenProvider.validateToken("token")).thenReturn(Mono.empty());

                StepVerifier.create(tokenAuthFilter.filter(serverWebExchange, chain))
                    .expectComplete()
                    .verify();

                verify(chain, never()).filter(serverWebExchange);
            }

            @Test
            void givenTokenIsInvalidEmptyUser_thenContinueChain() {
                mockTokenInCookie();
                when(tokenProvider.validateToken("token")).thenReturn(Mono.just(new QueryResponse()));
                when(chain.filter(any())).thenReturn(Mono.empty());

                StepVerifier.create(tokenAuthFilter.filter(serverWebExchange, chain))
                    .expectComplete()
                    .verify();

                verify(chain, times(1)).filter(any());
            }
        }

        @Nested
        class WhenTokenIsNotAvailable {

            @BeforeEach
            void setUp() {
                var cookieProperties = mock(CookieProperties.class);
                when(authConfigurationProperties.getCookieProperties()).thenReturn(cookieProperties);
                when(cookieProperties.getCookieName()).thenReturn(COOKIE_NAME);
            }

            @Test
            void thenContinueChain() {
                when(httpRequest.getHeaders()).thenReturn(HttpHeaders.EMPTY);
                tokenAuthFilter.filter(serverWebExchange, chain);
                verify(chain, times(1)).filter(any());
            }

        }

    }

}
