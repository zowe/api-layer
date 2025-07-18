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
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.web.server.WebFilterChain;
import org.zowe.apiml.security.common.token.X509AuthenticationToken;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.ContextView;

import java.security.cert.X509Certificate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class X509AuthFilterTest {

    @Mock private Authentication authentication;
    @Mock private ReactiveAuthenticationManager x509AuthProvider;
    @Mock private WebFilterChain chain;

    private X509AuthFilter filter;

    private MockServerWebExchange exchange;
    private MockServerHttpRequest request;

    @BeforeEach
    void setUp() {
        ReactiveSecurityContextHolder.withAuthentication(authentication);
        filter = new X509AuthFilter(x509AuthProvider);
        request = MockServerHttpRequest.get("/someresource").build();
        exchange = MockServerWebExchange.from(request);
        when(chain.filter(exchange)).thenReturn(Mono.empty());
    }

    @Nested
    class GivenFilter {

        private Mono<Void> testMono;

        @BeforeEach
        void setUp() {
            var securityContext = new SecurityContextImpl(authentication);
            var contextMono = Mono.just(securityContext);

            testMono = filter.filter(exchange, chain)
                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(contextMono));
        }

        @Test
        void whenAuthenticationComplete_thenContinue() {
            testMono = filter.filter(exchange, chain)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));

            StepVerifier.create(testMono)
                .expectComplete()
                .verify();

            verifyNoInteractions(x509AuthProvider);
        }

        @Test
        void whenCertsNotProvided_thenContinue() {
            testMono = filter.filter(exchange, chain); // no auth context

            exchange.getAttributes().put("client.auth.X509Certificate", new X509Certificate[]{});


            StepVerifier.create(testMono)
                .expectComplete()
                .verify();

            verifyNoInteractions(x509AuthProvider);
        }

        @Nested
        class GivenCertsProvided {

            @Mock X509Certificate cert;
            private X509Certificate[] certs;

            @BeforeEach
            void setUp() {
                certs = new X509Certificate[]{ cert };
                request = MockServerHttpRequest.get("/someresource").build();
                exchange = MockServerWebExchange.from(request);
                exchange.getAttributes().put("client.auth.X509Certificate", certs);
            }

            @SuppressWarnings("unchecked")
            @Test
            void whenCertsValid_writeContext() {
                testMono = filter.filter(exchange, chain);

                when(x509AuthProvider.authenticate(argThat(auth -> auth instanceof X509AuthenticationToken token && token.getCredentials() == certs)))
                    .thenReturn(Mono.just(authentication));

                when(authentication.isAuthenticated()).thenReturn(true);
                var mockMono = mock(Mono.class);
                when(chain.filter(exchange)).thenReturn(mockMono);
                when(mockMono.contextWrite(any(ContextView.class))).thenReturn(Mono.empty());

                StepVerifier.create(testMono)
                    .expectComplete()
                    .verify();
            }

            @Test
            void whenCertsNotValid_thenContinue() {
                testMono = filter.filter(exchange, chain);

                when(x509AuthProvider.authenticate(argThat(auth -> auth instanceof X509AuthenticationToken token && token.getCredentials() == certs)))
                    .thenReturn(Mono.just(authentication));

                when(chain.filter(exchange)).thenReturn(Mono.empty());

                StepVerifier.create(testMono)
                    .expectComplete()
                    .verify();
            }

        }

    }

}
