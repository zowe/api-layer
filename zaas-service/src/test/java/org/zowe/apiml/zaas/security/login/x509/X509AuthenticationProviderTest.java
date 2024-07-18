/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.login.x509;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.zowe.apiml.zaas.security.mapping.AuthenticationMapper;
import org.zowe.apiml.zaas.security.service.TokenCreationService;
import org.zowe.apiml.zaas.security.service.schema.source.X509AuthSource;
import org.zowe.apiml.zaas.utils.X509Utils;
import org.zowe.apiml.security.common.token.X509AuthenticationToken;

import java.security.cert.X509Certificate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class X509AuthenticationProviderTest {

    private AuthenticationMapper mapper;
    private TokenCreationService tokenCreationService;
    private X509AuthenticationProvider x509AuthenticationProvider;

    private final X509Certificate[] x509Certificate = new X509Certificate[]{
        X509Utils.getCertificate(X509Utils.correctBase64("zowe"), "CN=user"),
    };

    private final X509AuthSource x509AuthSource = new X509AuthSource(x509Certificate[0]);

    @BeforeEach
    void setUp() {
        mapper = mock(AuthenticationMapper.class);
        tokenCreationService = mock(TokenCreationService.class);
        x509AuthenticationProvider = new X509AuthenticationProvider(mapper, tokenCreationService);
        x509AuthenticationProvider.isClientCertEnabled = true;
    }

    @Test
    void x509AuthenticationIsSupported() {
        assertTrue(x509AuthenticationProvider.supports(X509AuthenticationToken.class));
    }

    @Test
    void givenX509AuthIsDisabled_whenRequested_thenNullIsReturned() {
        x509AuthenticationProvider.isClientCertEnabled = false;
        assertNull(x509AuthenticationProvider.authenticate(new X509AuthenticationToken(x509Certificate)));
    }


    @Test
    void givenCertificateIsntMappedToUser_whenAuthenticationIsRequired_thenNullIsReturned() {
        when(mapper.mapToMainframeUserId(x509AuthSource)).thenReturn(null);
        Authentication result = x509AuthenticationProvider.authenticate(new X509AuthenticationToken(x509Certificate));
        assertThat(result, is(nullValue()));
    }


    @Test
    void givenZosmfIsPresent_whenValidCertificateAndPassTicketGenerate_returnZosmfJwtToken() {
        String validUsername = "validUsername";

        when(mapper.mapToMainframeUserId(x509AuthSource)).thenReturn(validUsername);
        when(tokenCreationService.createJwtTokenWithoutCredentials(validUsername)).thenReturn("validJwtToken");

        Authentication result = x509AuthenticationProvider.authenticate(new X509AuthenticationToken(x509Certificate));
        assertThat(result.isAuthenticated(), is(true));
    }
}
