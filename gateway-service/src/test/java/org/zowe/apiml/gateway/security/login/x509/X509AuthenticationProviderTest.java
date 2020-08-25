/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.login.x509;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.gateway.utils.X509Utils;
import org.zowe.apiml.security.common.error.AuthenticationTokenException;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import org.zowe.apiml.security.common.token.X509AuthenticationToken;

import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class X509AuthenticationProviderTest {

    private X509Authentication x509Authentication;
    private AuthenticationService authenticationService;
    private X509AuthenticationProvider x509AuthenticationProvider;

    private X509Certificate[] x509Certificate = new X509Certificate[]{
        X509Utils.getCertificate(X509Utils.correctBase64("zowe"), "CN=user"),
    };

    @BeforeEach
    void setUp() {
        x509Authentication = mock(X509Authentication.class);
        when(x509Authentication.verifyCertificate(x509Certificate[0])).thenReturn("user");
        authenticationService = mock(AuthenticationService.class);
        when(authenticationService.createJwtToken("user", "security-domain", null)).thenReturn("jwt");
        when(authenticationService.createTokenAuthentication("user", "jwt")).thenReturn(new TokenAuthentication("user", "jwt"));
        x509AuthenticationProvider = new X509AuthenticationProvider(x509Authentication, authenticationService);
    }

    @Test
    void whenProvidedCertificate_shouldReturnToken() {
        TokenAuthentication token = (TokenAuthentication) x509AuthenticationProvider.authenticate(new X509AuthenticationToken(x509Certificate));
        assertEquals("jwt", token.getCredentials());
    }

    @Test
    void whenWrongTokenProvided_ThrowException() {
        TokenAuthentication token = new TokenAuthentication("user", "user");
        AuthenticationTokenException exception = assertThrows(AuthenticationTokenException.class, () -> x509AuthenticationProvider.authenticate(token));
        assertEquals("Wrong authentication token. " + TokenAuthentication.class, exception.getMessage());
    }
}
