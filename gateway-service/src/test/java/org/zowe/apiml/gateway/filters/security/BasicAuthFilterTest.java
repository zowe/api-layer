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
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.util.CollectionUtils.toMultiValueMap;

@ExtendWith(MockitoExtension.class)
public class BasicAuthFilterTest {

    private static final String VALID_BASIC_CREDENTIALS = "dXNlcjpwYXNz";

    @Mock private BasicAuthProvider basicAuthProvider;

    @Mock private WebFilterChain chain;

    @Mock private ServerWebExchange serverWebExchange;

    @Mock private ServerHttpRequest httpRequest;

    private BasicAuthFilter basicAuthFilter;
    private MockServerHttpResponse response;

    @Nested
    class GivenBasicAuthFilter {

        @BeforeEach
        void setUp() {
            when(serverWebExchange.getRequest()).thenReturn(httpRequest);
            basicAuthFilter = new BasicAuthFilter(basicAuthProvider);
            response = new MockServerHttpResponse();
            lenient().when(serverWebExchange.getResponse()).thenReturn(response);
        }

        @Nested
        class WhenBasicCredentialsAreAvailable {

            private void mockBasicAuth(String credentials) {
                when(httpRequest.getHeaders())
                    .thenReturn(new HttpHeaders(
                        toMultiValueMap(
                            singletonMap("Authorization", asList(
                                "Basic " + credentials
                            ))
                        )
                    ));
            }

            @SuppressWarnings("unchecked")
            @Test
            void givenAuthIsValid_whenFilter_thenAuthenticate() {
                mockBasicAuth(VALID_BASIC_CREDENTIALS);
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
                mockBasicAuth(VALID_BASIC_CREDENTIALS);
                when(basicAuthProvider.getToken("Basic " + VALID_BASIC_CREDENTIALS))
                    .thenReturn(Mono.error(new RuntimeException("invalid credentials")));

                StepVerifier.create(basicAuthFilter.filter(serverWebExchange, chain))
                    .expectErrorSatisfies(e -> assertEquals("invalid credentials", e.getMessage()))
                    .verify();

                verify(chain, never()).filter(serverWebExchange);
            }

            @Test
            void givenAuthIsInvalidEmpty_whenFilter_thenDontContinue() {
                mockBasicAuth(VALID_BASIC_CREDENTIALS);
                when(basicAuthProvider.getToken("Basic " + VALID_BASIC_CREDENTIALS))
                    .thenReturn(Mono.empty());

                StepVerifier.create(basicAuthFilter.filter(serverWebExchange, chain))
                    .expectComplete()
                    .verify();

                verify(chain, never()).filter(serverWebExchange);
            }

            @Test
            void givenAuthIsInalidBase64_whenFilter_thenUnauthorized() {
                mockBasicAuth("INVALID_BASE64");

                StepVerifier.create(basicAuthFilter.filter(serverWebExchange, chain))
                    .expectComplete()
                    .verify();

                assertEquals(HttpStatusCode.valueOf(401), response.getStatusCode());
                verifyNoInteractions(basicAuthProvider);
            }

            @Test
            void givenAuthIsInvalidFormat_whenFilter_thenUnauthorized() {
                mockBasicAuth("dXNlcnBhc3M="); //userpass (no colon)

                StepVerifier.create(basicAuthFilter.filter(serverWebExchange, chain))
                    .expectComplete()
                    .verify();

                assertEquals(HttpStatusCode.valueOf(401), response.getStatusCode());
                verifyNoInteractions(basicAuthProvider);
            }

            @Test
            void givenAuthIsValidFormat_whenEmptyToken_thenUnauthorized() {
                mockBasicAuth(VALID_BASIC_CREDENTIALS);

                when(basicAuthProvider.getToken("Basic " + VALID_BASIC_CREDENTIALS))
                    .thenReturn(Mono.just(""));

                StepVerifier.create(basicAuthFilter.filter(serverWebExchange, chain))
                    .expectComplete()
                    .verify();

                assertEquals(HttpStatusCode.valueOf(401), response.getStatusCode());
                verifyNoInteractions(chain);
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
