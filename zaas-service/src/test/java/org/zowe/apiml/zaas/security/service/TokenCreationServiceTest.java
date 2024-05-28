/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.zowe.apiml.zaas.security.login.Providers;
import org.zowe.apiml.zaas.security.login.zosmf.ZosmfAuthenticationProvider;
import org.zowe.apiml.zaas.security.service.saf.SafIdtException;
import org.zowe.apiml.zaas.security.service.saf.SafIdtProvider;
import org.zowe.apiml.zaas.security.service.zosmf.ZosmfService;
import org.zowe.apiml.passticket.IRRPassTicketGenerationException;
import org.zowe.apiml.passticket.PassTicketService;
import org.zowe.apiml.security.common.error.AuthenticationTokenException;
import org.zowe.apiml.security.common.token.TokenAuthentication;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.zowe.apiml.zaas.security.service.zosmf.ZosmfService.TokenType.JWT;
import static org.zowe.apiml.zaas.security.service.zosmf.ZosmfService.TokenType.LTPA;

@ExtendWith(MockitoExtension.class)
class TokenCreationServiceTest {

    private TokenCreationService underTest;

    @Mock
    private PassTicketService passTicketService;

    @Mock
    private ZosmfAuthenticationProvider zosmfAuthenticationProvider;

    @Mock
    private Providers providers;

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private ZosmfService zosmfService;

    @Mock
    private SafIdtProvider safIdtProvider;

    private final String VALID_USER_ID = "validUserId";
    private final String VALID_ZOSMF_TOKEN = "validZosmfToken";
    private final String VALID_APIML_TOKEN = "validApimlToken";
    private final String PASSTICKET = "passTicket";
    private final String VALID_ZOSMF_APPLID = "IZUDFLT";
    private final String VALID_SAFIDT = "validSAFIdentityToken";

    @BeforeEach
    void setUp() {
        underTest = new TokenCreationService(providers, Optional.of(zosmfAuthenticationProvider), zosmfService, passTicketService, authenticationService, safIdtProvider);
        underTest.zosmfApplId = "IZUDFLT";
    }

    @Test
    void givenZosmfIsUnavailable_whenTokenIsRequested_thenTokenCreatedByApiMlIsReturned() {
        when(providers.isZosfmUsed()).thenReturn(false);
        when(authenticationService.createJwtToken(eq(VALID_USER_ID), any(), any())).thenReturn(VALID_APIML_TOKEN);
        when(authenticationService.createTokenAuthentication(VALID_USER_ID, VALID_APIML_TOKEN)).thenReturn(new TokenAuthentication(VALID_USER_ID, VALID_APIML_TOKEN));

        String jwtToken = underTest.createJwtTokenWithoutCredentials(VALID_USER_ID);
        assertThat(jwtToken, is(VALID_APIML_TOKEN));
    }

    @Test
    void givenZosmfIsntPresentBecauseOfError_whenTokenIsRequested_shouldReturnTokenCreatedByApiMl() {
        when(providers.isZosfmUsed()).thenThrow(new AuthenticationServiceException("zOSMF id invalid"));
        when(authenticationService.createJwtToken(eq(VALID_USER_ID), any(), any())).thenReturn(VALID_APIML_TOKEN);
        when(authenticationService.createTokenAuthentication(VALID_USER_ID, VALID_APIML_TOKEN)).thenReturn(new TokenAuthentication(VALID_USER_ID, VALID_APIML_TOKEN));

        String jwtToken = underTest.createJwtTokenWithoutCredentials(VALID_USER_ID);
        assertThat(jwtToken, is(VALID_APIML_TOKEN));
    }

    @Test
    void givenZosmfIsAvailable_whenTokenIsRequested_thenTokenCreatedByZosmfIsReturned() throws IRRPassTicketGenerationException {
        when(providers.isZosmfAvailable()).thenReturn(true);
        when(providers.isZosfmUsed()).thenReturn(true);
        when(passTicketService.generate(VALID_USER_ID, VALID_ZOSMF_APPLID)).thenReturn(PASSTICKET);
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

    @Test
    void givenNoZosmf_whenCreatingZosmfToken_thenReturnEmptyResult() {
        when(providers.isZosfmUsed()).thenReturn(false);

        Map<ZosmfService.TokenType, String> tokens = underTest.createZosmfTokensWithoutCredentials("user");

        assertTrue(tokens.isEmpty());
    }

    @Test
    void givenZosmfAvailable_whenCreatingZosmfToken_thenReturnEmptyResult() throws IRRPassTicketGenerationException {
        when(providers.isZosfmUsed()).thenReturn(true);
        when(providers.isZosmfAvailable()).thenReturn(true);
        when(passTicketService.generate(VALID_USER_ID, VALID_ZOSMF_APPLID)).thenReturn(PASSTICKET);

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(VALID_USER_ID, PASSTICKET);
        Map<ZosmfService.TokenType, String> expectedTokens = new HashMap<ZosmfService.TokenType, String>() {{
            put(LTPA, "ltpaToken");
            put(JWT, "jwtToken");
        }};
        ZosmfService.AuthenticationResponse authenticationResponse = new ZosmfService.AuthenticationResponse("domain", expectedTokens);
        when(zosmfService.authenticate(authToken)).thenReturn(authenticationResponse);

        Map<ZosmfService.TokenType, String> tokens = underTest.createZosmfTokensWithoutCredentials(VALID_USER_ID);

        assertEquals(expectedTokens, tokens);
    }

    @Test
    void givenPassTicketGenerated_whenCreatingSafIdToken_thenTokenReturned() throws IRRPassTicketGenerationException {
        when(passTicketService.generate(VALID_USER_ID, VALID_ZOSMF_APPLID)).thenReturn(PASSTICKET);
        when(safIdtProvider.generate(VALID_USER_ID, PASSTICKET.toCharArray(), VALID_ZOSMF_APPLID)).thenReturn(VALID_SAFIDT);

        String safIdt = underTest.createSafIdTokenWithoutCredentials(VALID_USER_ID, VALID_ZOSMF_APPLID);

        assertEquals(VALID_SAFIDT, safIdt);
    }

    @Test
    void givenPassTicketException_whenCreatingSafIdToken_thenExceptionThrown() throws IRRPassTicketGenerationException {
        when(passTicketService.generate(VALID_USER_ID, VALID_ZOSMF_APPLID)).thenThrow(new IRRPassTicketGenerationException(8, 8, 8));

        Exception e = assertThrows(IRRPassTicketGenerationException.class, () -> {
                underTest.createSafIdTokenWithoutCredentials(VALID_USER_ID, VALID_ZOSMF_APPLID);
            });

        assertEquals("Error on generation of PassTicket: An internal error was encountered.", e.getMessage());
    }

    @Test
    void givenSafIdtException_whenCreatingSafIdToken_thenExceptionThrown() throws IRRPassTicketGenerationException {
        when(passTicketService.generate(VALID_USER_ID, VALID_ZOSMF_APPLID)).thenReturn(PASSTICKET);
        when(safIdtProvider.generate(VALID_USER_ID, PASSTICKET.toCharArray(), VALID_ZOSMF_APPLID)).thenThrow(new SafIdtException("Test exception"));

        Exception e = assertThrows(SafIdtException.class, () -> {
            underTest.createSafIdTokenWithoutCredentials(VALID_USER_ID, VALID_ZOSMF_APPLID);
        });

        assertEquals("Test exception", e.getMessage());
    }
}
