/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.service.schema.source;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.zowe.apiml.security.common.token.AccessTokenProvider;
import org.zowe.apiml.security.common.token.QueryResponse;
import org.zowe.apiml.security.common.token.TokenExpireException;
import org.zowe.apiml.security.common.token.TokenNotValidException;
import org.zowe.apiml.zaas.security.service.AuthenticationService;
import org.zowe.apiml.zaas.security.service.TokenCreationService;

import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.zowe.apiml.zaas.security.service.schema.source.PATAuthSourceService.SERVICE_ID_HEADER;

@ExtendWith(MockitoExtension.class)
class PATAuthSourceServiceTest {

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private AccessTokenProvider tokenProvider;

    @Mock
    private TokenCreationService tokenCreationService;

    private PATAuthSourceService patAuthSourceService;

    private static final String TOKEN = "token";

    @BeforeEach
    void setUp() {
        patAuthSourceService = new PATAuthSourceService(authenticationService, tokenProvider, tokenCreationService);
    }

    @Test
    void returnPATSourceMapper() {
        assertTrue(patAuthSourceService.getMapper().apply("token") instanceof PATAuthSource);
    }

    @Nested
    class GivenValidTokenTest {

        @Test
        void givenPatTokenInRequestContext_thenReturnTheToken() {
            HttpServletRequest request = new MockHttpServletRequest();
            when(authenticationService.getPATFromRequest(request)).thenReturn(Optional.of(TOKEN));
            when(authenticationService.getTokenOrigin(TOKEN)).thenReturn(AuthSource.Origin.ZOWE_PAT);
            Optional<String> tokenResult = patAuthSourceService.getToken(request);
            assertTrue(tokenResult.isPresent());
            assertEquals(TOKEN, tokenResult.get());
        }

        @Test
        void givenZoweTokenInRequestContext_thenReturnEmpty() {
            HttpServletRequest request = new MockHttpServletRequest();
            when(authenticationService.getJwtTokenFromRequest(request)).thenReturn(Optional.of(TOKEN));
            when(authenticationService.getTokenOrigin(TOKEN)).thenReturn(AuthSource.Origin.ZOWE);

            assertFalse(patAuthSourceService.getToken(request).isPresent());
        }

        @Test
        void givenScopeFromHeader_whenIsValid_thenReturnTheToken() {
            String serviceId = "gateway";
            when(tokenProvider.isValidForScopes(TOKEN, serviceId)).thenReturn(true);
            PATAuthSource authSource = new PATAuthSource(TOKEN);
            authSource.setDefaultServiceId(serviceId);

            assertTrue(patAuthSourceService.isValid(authSource));
        }

        @Test
        void whenGetAuthSourceFromRequest_thenReturnAuthSource() {
            String serviceId = "gateway";

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(SERVICE_ID_HEADER, serviceId);

            when(authenticationService.getPATFromRequest(request)).thenReturn(Optional.of(TOKEN));
            when(authenticationService.getTokenOrigin(TOKEN)).thenReturn(AuthSource.Origin.ZOWE_PAT);

            Optional<AuthSource> authSource = patAuthSourceService.getAuthSourceFromRequest(request);

            assertTrue(authSource.isPresent());
            PATAuthSource patAuthSource = (PATAuthSource) authSource.get();
            assertEquals(serviceId, patAuthSource.getDefaultServiceId());
            assertEquals(TOKEN, patAuthSource.getSource());
        }

    }

    @Nested
    class GivenInvalidTokenTest {
        @Test
        void whenExceptionIsThrown_thenReturnTokenInvalid() {
            String serviceId = "gateway";
            //when(context.get(SERVICE_ID_KEY)).thenReturn(serviceId);
            when(tokenProvider.isValidForScopes(TOKEN, serviceId)).thenThrow(new RuntimeException());
            PATAuthSource authSource = new PATAuthSource(TOKEN);

            assertFalse(patAuthSourceService.isValid(authSource));
        }

        @Test
        void whenNoScope_thenReturnTokenInvalid() {
            PATAuthSource authSource = new PATAuthSource(TOKEN);

            assertFalse(patAuthSourceService.isValid(authSource));
        }

        @Test
        void whenTokenIsExpired_thenThrow() {
            HttpServletRequest request = new MockHttpServletRequest();
            when(authenticationService.getJwtTokenFromRequest(request)).thenReturn(Optional.of(TOKEN));
            when(authenticationService.getTokenOrigin(TOKEN)).thenThrow(new TokenExpireException("token expired"));

            assertThrows(TokenExpireException.class, () -> patAuthSourceService.getToken(request));
            verify(authenticationService, times(1)).getTokenOrigin(TOKEN);
        }

        @Test
        void whenTokenIsNotValid_thenThrow() {
            HttpServletRequest request = new MockHttpServletRequest();
            when(authenticationService.getJwtTokenFromRequest(request)).thenReturn(Optional.of(TOKEN));
            when(authenticationService.getTokenOrigin(TOKEN)).thenThrow(new TokenNotValidException("token not valid"));

            assertThrows(TokenNotValidException.class, () -> patAuthSourceService.getToken(request));
            verify(authenticationService, times(1)).getTokenOrigin(TOKEN);
        }

    }


    @Nested
    class GivenDifferentAuthSourcesTest {
        @Test
        void givenPATAuthSource_thenReturnCorrectUserInfo() {
            PATAuthSource authSource = new PATAuthSource(TOKEN);
            QueryResponse response = new QueryResponse(null, "user", new Date(), new Date(), "issuer", null, QueryResponse.Source.ZOWE_PAT);
            when(authenticationService.parseJwtWithSignature(TOKEN)).thenReturn(response);
            AuthSource.Parsed parsedSource = patAuthSourceService.parse(authSource);
            assertEquals(response.getUserId(), parsedSource.getUserId());
        }

        @Test
        void givenJWTAuthSource_thenReturnNull() {
            JwtAuthSource authSource = new JwtAuthSource(TOKEN);
            AuthSource.Parsed parsedSource = patAuthSourceService.parse(authSource);
            assertNull(parsedSource);
        }

        @Test
        void givenValidAuthSource_thenReturnLTPAToken() {
            String ltpa = "ltpa";
            PATAuthSource authSource = new PATAuthSource(TOKEN);
            QueryResponse response = new QueryResponse(null, "user", new Date(), new Date(), "issuer", null, QueryResponse.Source.ZOWE);
            when(authenticationService.parseJwtWithSignature(TOKEN)).thenReturn(response);
            when(tokenCreationService.createJwtTokenWithoutCredentials(response.getUserId())).thenReturn(TOKEN);
            when(authenticationService.getTokenOrigin(TOKEN)).thenReturn(AuthSource.Origin.ZOWE);
            when(authenticationService.getLtpaToken(TOKEN)).thenReturn(ltpa);
            String ltpaResult = patAuthSourceService.getLtpaToken(authSource);
            assertEquals(ltpa, ltpaResult);
        }

        @Test
        void givenValidAuthSource_thenReturnJWT() {
            PATAuthSource authSource = new PATAuthSource(TOKEN);
            QueryResponse response = new QueryResponse(null, "user", new Date(), new Date(), "issuer", null, QueryResponse.Source.ZOWE_PAT);
            when(authenticationService.parseJwtWithSignature(TOKEN)).thenReturn(response);
            when(tokenCreationService.createJwtTokenWithoutCredentials(response.getUserId())).thenReturn(TOKEN);
            String jwt = patAuthSourceService.getJWT(authSource);
            assertEquals(TOKEN, jwt);
        }
    }

}
