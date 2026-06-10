/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.handler.FailedAuthenticationWebHandler;
import org.zowe.apiml.handler.LocalTokenProvider;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.token.OIDCProvider;
import org.zowe.apiml.security.common.verify.CertificateValidator;
import org.zowe.apiml.util.HttpUtils;
import org.zowe.apiml.zaas.security.config.CompoundAuthProvider;
import org.zowe.apiml.zaas.security.login.x509.X509AuthenticationProvider;
import org.zowe.apiml.zaas.security.mapping.AuthenticationMapper;
import org.zowe.apiml.zaas.security.query.TokenAuthenticationProvider;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

/**
 * Verifies ticketFilter() and refreshTokenFilter() are created with OIDC support wired in.
 * Focuses on verifying the filter chain beans construct without error — the actual
 * OIDC integration behavior is tested by the OidcOauth2Test integration tests.
 */
@ExtendWith(MockitoExtension.class)
class WebSecurityConfigTest {

    @Mock private CompoundAuthProvider compoundAuthProvider;
    @Mock private X509AuthenticationProvider x509AuthenticationProvider;
    @Mock private LocalTokenProvider localTokenProvider;
    @Mock private CertificateValidator certificateValidator;
    @Mock private FailedAuthenticationWebHandler failedAuthenticationWebHandler;
    @Mock private TokenAuthenticationProvider tokenAuthenticationProvider;
    @Mock private HttpUtils httpUtils;
    @Mock private OIDCProvider oidcProvider;
    @Mock private AuthenticationMapper oidcMapper;
    @Mock private ServerHttpSecurity http;

    private final Set<String> publicKeyCertificatesBase64 = Set.of();
    private AuthConfigurationProperties authConfigProps;
    private WebSecurityConfig config;

    @BeforeEach
    void setUp() {
        authConfigProps = new AuthConfigurationProperties();
        config = new WebSecurityConfig(
            compoundAuthProvider,
            x509AuthenticationProvider,
            localTokenProvider,
            publicKeyCertificatesBase64,
            certificateValidator,
            failedAuthenticationWebHandler,
            tokenAuthenticationProvider,
            httpUtils
        );
        config.setOidcProvider(oidcProvider);
        config.setOidcMapper(oidcMapper);

        ReflectionTestUtils.setField(config, "gatewayPort", 10021);
        ReflectionTestUtils.setField(config, "verifySslCertificatesOfServices", true);
        ReflectionTestUtils.setField(config, "isOidcEnabled", true);
        ReflectionTestUtils.setField(config, "oidcUserIdFieldPath", "sub");

        // Stub ServerHttpSecurity to return itself for fluent chaining
        lenient().when(http.csrf(any())).thenReturn(http);
        lenient().when(http.headers(any())).thenReturn(http);
        lenient().when(http.x509(any())).thenReturn(http);
        lenient().when(http.securityMatcher(any())).thenReturn(http);
        lenient().when(http.authorizeExchange(any())).thenReturn(http);
        lenient().when(http.httpBasic(any())).thenReturn(http);
        lenient().when(http.addFilterAfter(any(), any())).thenReturn(http);
        lenient().when(http.addFilterBefore(any(), any())).thenReturn(http);
        lenient().when(http.exceptionHandling(any())).thenReturn(http);
        lenient().when(http.build()).thenReturn(null);
    }

    @Test
    void ticketFilterCreatesSuccessfullyWithOidcEnabled() {
        SecurityWebFilterChain chain = config.ticketFilter(http, authConfigProps);
        assertThat(chain).isNull(); // build() returns null in mock — no NPE = success
    }

    @Test
    void refreshTokenFilterCreatesSuccessfullyWithOidcEnabled() {
        SecurityWebFilterChain chain = config.refreshTokenFilter(http, authConfigProps);
        assertThat(chain).isNull();
    }

    @Test
    void ticketFilterCreatesSuccessfullyWithOidcDisabled() {
        ReflectionTestUtils.setField(config, "isOidcEnabled", false);
        SecurityWebFilterChain chain = config.ticketFilter(http, authConfigProps);
        assertThat(chain).isNull();
    }

    @Test
    void refreshTokenFilterCreatesSuccessfullyWithOidcDisabled() {
        ReflectionTestUtils.setField(config, "isOidcEnabled", false);
        SecurityWebFilterChain chain = config.refreshTokenFilter(http, authConfigProps);
        assertThat(chain).isNull();
    }
}
