/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.zowe.apiml.gateway.security.login.Providers;
import org.zowe.apiml.gateway.security.login.zosmf.ZosmfAuthenticationProvider;
import org.zowe.apiml.passticket.IRRPassTicketGenerationException;
import org.zowe.apiml.passticket.PassTicketService;
import org.zowe.apiml.security.common.error.AuthenticationTokenException;
import org.zowe.apiml.security.common.token.TokenAuthentication;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TokenCreationServiceTest {
    private TokenCreationService underTest;

    private PassTicketService passTicketService;
    private ZosmfAuthenticationProvider zosmfAuthenticationProvider;
    private Providers providers;
    private AuthenticationService authenticationService;

    private final String VALID_USER_ID = "validUserId";
    private final String VALID_ZOSMF_TOKEN = "validZosmfToken";
    private final String VALID_APIML_TOKEN = "validApimlToken";
    private final String VALID_ZOSMF_APPLID = "IZUDFLT";

    @BeforeEach
    void setUp() {
        passTicketService = mock(PassTicketService.class);
        zosmfAuthenticationProvider = mock(ZosmfAuthenticationProvider.class);
        providers = mock(Providers.class);
        authenticationService = mock(AuthenticationService.class);

        underTest = new TokenCreationService(providers, zosmfAuthenticationProvider, passTicketService, authenticationService);
        underTest.zosmfApplId = "IZUDFLT";
    }

    @Test
    void givenZosmfIsUnavailable_whenTokenIsRequested_thenTokenCreatedByApiMlIsReturned() {
        when(providers.isZosmfAvailable()).thenReturn(false);
        when(authenticationService.createJwtToken(eq(VALID_USER_ID), any(), any())).thenReturn(VALID_APIML_TOKEN);
        when(authenticationService.createTokenAuthentication(VALID_USER_ID, VALID_APIML_TOKEN)).thenReturn(new TokenAuthentication(VALID_USER_ID, VALID_APIML_TOKEN));

        String jwtToken = underTest.createJwtTokenWithoutCredentials(VALID_USER_ID);
        assertThat(jwtToken, is(VALID_APIML_TOKEN));
    }

    @Test
    void givenZosmfIsntPresentBecauseOfError_whenTokenIsRequested_shouldReturnTokenCreatedByApiMl() {
        when(providers.isZosmfAvailable()).thenThrow(new AuthenticationServiceException("zOSMF id invalid"));
        when(authenticationService.createJwtToken(eq(VALID_USER_ID), any(), any())).thenReturn(VALID_APIML_TOKEN);
        when(authenticationService.createTokenAuthentication(VALID_USER_ID, VALID_APIML_TOKEN)).thenReturn(new TokenAuthentication(VALID_USER_ID, VALID_APIML_TOKEN));

        String jwtToken = underTest.createJwtTokenWithoutCredentials(VALID_USER_ID);
        assertThat(jwtToken, is(VALID_APIML_TOKEN));
    }

    @Test
    void givenZosmfIsAvailable_whenTokenIsRequested_thenTokenCreatedByZosmfIsReturned() throws IRRPassTicketGenerationException {
        when(providers.isZosmfAvailable()).thenReturn(true);
        when(providers.isZosfmUsed()).thenReturn(true);
        when(passTicketService.generate(VALID_USER_ID, VALID_ZOSMF_APPLID)).thenReturn("validPassticket");
        when(zosmfAuthenticationProvider.authenticate(any())).thenReturn(new TokenAuthentication(VALID_USER_ID, VALID_ZOSMF_TOKEN));

        String jwtToken = underTest.createJwtTokenWithoutCredentials(VALID_USER_ID);
        assertThat(jwtToken, is(VALID_ZOSMF_TOKEN));
    }

    @Test
    void givenZosmfIsAvailableButPassticketGenerationFails_whenTokenIsRequested_thenExceptionIsThrown() throws IRRPassTicketGenerationException {
        when(providers.isZosmfAvailable()).thenReturn(true);
        when(providers.isZosfmUsed()).thenReturn(true);
        when(passTicketService.generate(VALID_USER_ID, VALID_ZOSMF_APPLID)).thenThrow(new IRRPassTicketGenerationException(4, 4, 4));

        assertThrows(AuthenticationTokenException.class,
            () -> underTest.createJwtTokenWithoutCredentials(VALID_USER_ID)
        );
    }
}
