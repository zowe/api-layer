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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.token.OIDCProvider;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import org.zowe.apiml.security.common.util.JWTTestUtils;
import org.zowe.apiml.zaas.security.mapping.AuthenticationMapper;
import org.zowe.apiml.zaas.security.service.schema.source.OIDCAuthSource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OIDCAuthFilterTest {

    private static final String MAINFRAME_USER = "TESTUSER";
    public static final String OIDC_TOKEN = JWTTestUtils.createDummyJwtToken(MAINFRAME_USER, "https://oidc.provider");
    private static final List<String> USER_ID_FIELD_PATH = List.of("sub");

    @Mock private OIDCProvider oidcProvider;
    @Mock private AuthenticationMapper oidcMapper;
    @Mock private WebFilterChain chain;

    private OIDCAuthFilter filter;
    private AuthConfigurationProperties authConfigurationProperties;

    @BeforeEach
    void setUp() {
        authConfigurationProperties = new AuthConfigurationProperties();
        filter = new OIDCAuthFilter(oidcProvider, oidcMapper, authConfigurationProperties, USER_ID_FIELD_PATH);
    }

    @Nested
    class WhenNoTokenPresent {

        @Test
        void thenPassThrough() {
            var request = MockServerHttpRequest.get("/apicatalog/api/v1/containers").build();
            var exchange = MockServerWebExchange.from(request);
            when(chain.filter(exchange)).thenReturn(Mono.empty());

            StepVerifier.create(filter.filter(exchange, chain))
                .expectComplete()
                .verify();

            verifyNoInteractions(oidcProvider);
            verifyNoInteractions(oidcMapper);
        }
    }

    @Nested
    class WhenTokenInBearerHeader {

        @Test
        void givenInvalidToken_thenPassThrough() {
            var request = MockServerHttpRequest.get("/apicatalog/api/v1/containers")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + OIDC_TOKEN)
                .build();
            var exchange = MockServerWebExchange.from(request);
            when(chain.filter(exchange)).thenReturn(Mono.empty());
            when(oidcProvider.isValid(OIDC_TOKEN)).thenReturn(false);

            StepVerifier.create(filter.filter(exchange, chain))
                .expectComplete()
                .verify();

            verify(oidcProvider).isValid(OIDC_TOKEN);
            verify(oidcMapper, never()).mapToMainframeUserId(any());
        }

        @Test
        void givenValidTokenAndSuccessfulMapping_thenAuthenticateAndStripToken() {
            var request = MockServerHttpRequest.get("/apicatalog/api/v1/containers")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + OIDC_TOKEN)
                .build();
            var exchange = MockServerWebExchange.from(request);
            when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
            when(oidcProvider.isValid(OIDC_TOKEN)).thenReturn(true);
            when(oidcMapper.mapToMainframeUserId(any(OIDCAuthSource.class))).thenReturn(MAINFRAME_USER);

            var result = filter.filter(exchange, chain);

            StepVerifier.create(result)
                .expectComplete()
                .verify();

            verify(oidcProvider).isValid(OIDC_TOKEN);
            verify(oidcMapper).mapToMainframeUserId(any(OIDCAuthSource.class));

            var captor = ArgumentCaptor.forClass(ServerWebExchange.class);
            verify(chain).filter(captor.capture());
            var passedExchange = captor.getValue();
            assertThat(passedExchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION)).isNull();
        }

        @Test
        void givenValidTokenButMappingFails_thenPassThrough() {
            var request = MockServerHttpRequest.get("/apicatalog/api/v1/containers")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + OIDC_TOKEN)
                .build();
            var exchange = MockServerWebExchange.from(request);
            when(chain.filter(exchange)).thenReturn(Mono.empty());
            when(oidcProvider.isValid(OIDC_TOKEN)).thenReturn(true);
            when(oidcMapper.mapToMainframeUserId(any(OIDCAuthSource.class))).thenReturn(null);

            StepVerifier.create(filter.filter(exchange, chain))
                .expectComplete()
                .verify();

            verify(oidcProvider).isValid(OIDC_TOKEN);
            verify(oidcMapper).mapToMainframeUserId(any(OIDCAuthSource.class));
        }
    }

    @Nested
    class WhenTokenInCookie {

        @Test
        void givenValidTokenInCookie_thenAuthenticateAndStripCookie() {
            String cookieName = authConfigurationProperties.getCookieProperties().getCookieName();
            var request = MockServerHttpRequest.get("/apicatalog/api/v1/containers")
                .cookie(new HttpCookie( cookieName, OIDC_TOKEN))
                .build();
            var exchange = MockServerWebExchange.from(request);
            when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
            when(oidcProvider.isValid(OIDC_TOKEN)).thenReturn(true);
            when(oidcMapper.mapToMainframeUserId(any(OIDCAuthSource.class))).thenReturn(MAINFRAME_USER);

            StepVerifier.create(filter.filter(exchange, chain))
                .expectComplete()
                .verify();

            verify(oidcProvider).isValid(OIDC_TOKEN);
            verify(oidcMapper).mapToMainframeUserId(any(OIDCAuthSource.class));

            var captor = ArgumentCaptor.forClass(ServerWebExchange.class);
            verify(chain).filter(captor.capture());
            var passedExchange = captor.getValue();
            assertThat(passedExchange.getRequest().getHeaders().get(HttpHeaders.COOKIE)).isNull();
        }
    }

    @Nested
    class WhenAlreadyAuthenticated {

        @Test
        void thenSkipOidcValidation() {
            var request = MockServerHttpRequest.get("/apicatalog/api/v1/containers")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + OIDC_TOKEN)
                .build();
            var exchange = MockServerWebExchange.from(request);
            when(chain.filter(exchange)).thenReturn(Mono.empty());

            Authentication existingAuth = TokenAuthentication.createAuthenticated("EXISTING_USER", JWTTestUtils.createDummyAPIMLToken("EXISTING_USER"), TokenAuthentication.Type.JWT);
            var securityContext = new SecurityContextImpl(existingAuth);

            var result = filter.filter(exchange, chain)
                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)));

            StepVerifier.create(result)
                .expectComplete()
                .verify();

            verifyNoInteractions(oidcProvider);
            verifyNoInteractions(oidcMapper);
        }
    }

}
