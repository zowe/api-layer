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
import org.springframework.security.core.Authentication;
import org.zowe.apiml.gateway.security.login.zosmf.ZosmfAuthenticationProvider;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.gateway.security.service.ZosmfService;
import org.zowe.apiml.gateway.utils.X509Utils;
import org.zowe.apiml.passticket.IRRPassTicketGenerationException;
import org.zowe.apiml.passticket.PassTicketService;
import org.zowe.apiml.security.common.error.AuthenticationTokenException;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import org.zowe.apiml.security.common.token.X509AuthenticationToken;

import java.security.cert.X509Certificate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class X509AuthenticationProviderTest {

    private X509Authentication x509Authentication;
    private AuthenticationService authenticationService;
    private X509AuthenticationProvider x509AuthenticationProvider;

    private PassTicketService passTicketService;
    private ZosmfAuthenticationProvider zosmfAuthenticationProvider;
    private ZosmfService zosmfService;

    private X509Certificate[] x509Certificate = new X509Certificate[]{
        X509Utils.getCertificate(X509Utils.correctBase64("zowe"), "CN=user"),
    };

    @BeforeEach
    void setUp() {
        x509Authentication = mock(X509Authentication.class);

        authenticationService = mock(AuthenticationService.class);
        passTicketService = mock(PassTicketService.class);
        zosmfAuthenticationProvider = mock(ZosmfAuthenticationProvider.class);
        zosmfService = mock(ZosmfService.class);
        when(authenticationService.createJwtToken("user", "security-domain", null)).thenReturn("jwt");
        when(authenticationService.createTokenAuthentication("user", "jwt")).thenReturn(new TokenAuthentication("user", "jwt"));
        x509AuthenticationProvider = new X509AuthenticationProvider(x509Authentication, authenticationService, passTicketService, zosmfAuthenticationProvider, zosmfService);
        x509AuthenticationProvider.isClientCertEnabled = true;
    }

    @Test
    void givenZosmfIsntPresent_whenProvidedCertificate_shouldReturnToken() {
        when(zosmfService.isAvailable()).thenReturn(false);
        when(x509Authentication.mapUserToCertificate(x509Certificate[0])).thenReturn("user");
        TokenAuthentication token = (TokenAuthentication) x509AuthenticationProvider.authenticate(new X509AuthenticationToken(x509Certificate));
        assertEquals("jwt", token.getCredentials());
    }

    @Test
    void givenZosmfIsntPresent_whenWrongTokenProvided_ThrowException() {
        when(zosmfService.isAvailable()).thenReturn(false);
        when(x509Authentication.mapUserToCertificate(x509Certificate[0])).thenReturn("user");
        TokenAuthentication token = new TokenAuthentication("user", "user");
        AuthenticationTokenException exception = assertThrows(AuthenticationTokenException.class, () -> x509AuthenticationProvider.authenticate(token));
        assertEquals("Wrong authentication token. " + TokenAuthentication.class, exception.getMessage());
    }

    @Test
    void x509AuthenticationIsSupported() {
        assertTrue(x509AuthenticationProvider.supports(X509AuthenticationToken.class));
    }

    @Test
    void givenZosmfIsntPresent_givenZosmfIsntPresent_whenCommonNameIsNotCorrect_returnNull() {
        when(zosmfService.isAvailable()).thenReturn(false);
        when(x509Authentication.mapUserToCertificate(x509Certificate[0])).thenReturn("wrong username");
        assertNull(x509AuthenticationProvider.authenticate(new X509AuthenticationToken(x509Certificate)));
    }

    @Test
    void givenZosmfIsPresent_whenValidCertificateAndPassTicketGenerate_returnZosmfJwtToken() throws IRRPassTicketGenerationException {
        String validUsername = "validUsername";
        String validZosmfApplId = "IZUDFLT";

        when(zosmfService.isAvailable()).thenReturn(true);
        when(x509Authentication.mapUserToCertificate(x509Certificate[0])).thenReturn(validUsername);
        when(passTicketService.generate(validUsername, validZosmfApplId)).thenReturn("validPassticket");
        Authentication authentication = new TokenAuthentication(validUsername, "validJwtToken");
        authentication.setAuthenticated(true);
        when(zosmfAuthenticationProvider.authenticate(any())).thenReturn(authentication);

        Authentication result = x509AuthenticationProvider.authenticate(new X509AuthenticationToken(x509Certificate));
        assertThat(result.isAuthenticated(), is(true));
    }

    @Test
    void givenZosmfIPresent_whenPassTicketGeneratesException_thenThrowAuthenticationException() throws IRRPassTicketGenerationException {
        String validUsername = "validUsername";

        when(zosmfService.isAvailable()).thenReturn(true);
        when(x509Authentication.mapUserToCertificate(x509Certificate[0])).thenReturn(validUsername);
        when(passTicketService.generate(validUsername, null)).thenThrow(new IRRPassTicketGenerationException(8,8,8));

        X509AuthenticationToken token = new X509AuthenticationToken(x509Certificate);
        assertThrows(AuthenticationTokenException.class, () -> x509AuthenticationProvider.authenticate(token));
    }

    @Test
    void givenCertificateIsntMappedToUser_whenAuthenticationIsRequired_thenNullIsReturned() {
        when(x509Authentication.mapUserToCertificate(x509Certificate[0])).thenReturn(null);
        Authentication result = x509AuthenticationProvider.authenticate(new X509AuthenticationToken(x509Certificate));
        assertThat(result, is(nullValue()));
    }
}
