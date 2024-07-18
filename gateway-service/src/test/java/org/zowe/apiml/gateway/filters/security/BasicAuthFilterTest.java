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
import org.zowe.apiml.gateway.service.BasicAuthProvider;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.function.Function;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.util.CollectionUtils.toMultiValueMap;

@ExtendWith(MockitoExtension.class)
public class BasicAuthFilterTest {

    @Mock
    private BasicAuthProvider basicAuthProvider;

    @Mock
    private WebFilterChain chain;

    @Mock
    private ServerWebExchange serverWebExchange;

    @Mock
    private ServerHttpRequest httpRequest;

    private BasicAuthFilter basicAuthFilter;

    @Nested
    class GivenBasicAuthFilter {

        @BeforeEach
        void setUp() {
            when(serverWebExchange.getRequest()).thenReturn(httpRequest);
            basicAuthFilter = new BasicAuthFilter(basicAuthProvider);
        }

        @Nested
        class WhenBasicCredentialsAreAvailable {

            private void mockBasicAuth() {
                when(httpRequest.getHeaders())
                    .thenReturn(new HttpHeaders(
                        toMultiValueMap(
                            singletonMap("Authorization", asList(
                                "Basic dXNlcjpwYXNz"
                            ))
                        )
                    ));
            }

            @SuppressWarnings("unchecked")
            @Test
            void givenAuthIsValid_whenFilter_thenAuthenticate() {
                mockBasicAuth();
                when(basicAuthProvider.getToken("Basic dXNlcjpwYXNz"))
                    .thenReturn(Mono.just("token"));
                Mono<Void> monoSpy = spy(Mono.empty());
                when(chain.filter(any())).thenReturn(monoSpy);

                StepVerifier.create(basicAuthFilter.filter(serverWebExchange, chain))
                    .expectComplete()
                    .verify();

                verify(monoSpy, times(1)).contextWrite(any(Function.class));
            }

            @Test
            void givenAuthIsInvalidError_whenFilter_thenError() {
                mockBasicAuth();
                when(basicAuthProvider.getToken("Basic dXNlcjpwYXNz"))
                    .thenReturn(Mono.error(new RuntimeException("invalid credentials")));

                StepVerifier.create(basicAuthFilter.filter(serverWebExchange, chain))
                    .expectErrorSatisfies(e -> assertEquals("invalid credentials", e.getMessage()))
                    .verify();

                verify(chain, never()).filter(serverWebExchange);
            }

            @Test
            void givenAuthIsInvalidEmpty_whenFilter_thenDontContinue() {
                mockBasicAuth();
                when(basicAuthProvider.getToken("Basic dXNlcjpwYXNz"))
                    .thenReturn(Mono.empty());

                StepVerifier.create(basicAuthFilter.filter(serverWebExchange, chain))
                    .expectComplete()
                    .verify();

                verify(chain, never()).filter(serverWebExchange);
            }

        }

        @Nested
        class WhenBasicCredentialsAreNotAvailable {

            @Test
            void whenFilter_thenContinueChain() {
                when(httpRequest.getHeaders()).thenReturn(HttpHeaders.EMPTY);
                basicAuthFilter.filter(serverWebExchange, chain);
                verify(chain, times(1)).filter(any());
            }

        }

    }

}
