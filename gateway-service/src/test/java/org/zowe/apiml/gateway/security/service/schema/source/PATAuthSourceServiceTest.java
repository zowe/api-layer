/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.security.service.schema.source;

import com.netflix.zuul.context.RequestContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.gateway.security.service.TokenCreationService;
import org.zowe.apiml.security.common.token.AccessTokenProvider;
import org.zowe.apiml.security.common.token.QueryResponse;
import org.zowe.apiml.security.common.token.TokenExpireException;
import org.zowe.apiml.security.common.token.TokenNotValidException;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVICE_ID_KEY;

class PATAuthSourceServiceTest {

    AuthenticationService authenticationService;
    AccessTokenProvider tokenProvider;
    TokenCreationService tokenCreationService;
    PATAuthSourceService patAuthSourceService;
    RequestContext context;
    String token = "token";

    @BeforeEach
    void setUp() {
        tokenProvider = mock(AccessTokenProvider.class);
        tokenCreationService = mock(TokenCreationService.class);
        authenticationService = mock(AuthenticationService.class);
        patAuthSourceService = new PATAuthSourceService(authenticationService, tokenProvider, tokenCreationService);
        context = mock(RequestContext.class);
        RequestContext.testSetCurrentContext(context);
    }

    @AfterAll
    static void cleanUp() {
        RequestContext.testSetCurrentContext(null);
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
            when(authenticationService.getPATFromRequest(request)).thenReturn(Optional.of(token));
            when(authenticationService.getTokenOrigin(token)).thenReturn(AuthSource.Origin.ZOWE_PAT);
            Optional<String> tokenResult = patAuthSourceService.getToken(request);
            assertTrue(tokenResult.isPresent());
            assertEquals(token, tokenResult.get());
        }

        @Test
        void givenZoweTokenInRequestContext_thenReturnEmpty() {
            HttpServletRequest request = new MockHttpServletRequest();
            when(authenticationService.getJwtTokenFromRequest(request)).thenReturn(Optional.of(token));
            when(authenticationService.getTokenOrigin(token)).thenReturn(AuthSource.Origin.ZOWE);
            assertFalse(patAuthSourceService.getToken(request).isPresent());
        }

        @Test
        void givenTokenInAuthSource_thenReturnValid() {
            String serviceId = "gateway";
            when(context.get(SERVICE_ID_KEY)).thenReturn(serviceId);
            when(tokenProvider.isValidForScopes(token, serviceId)).thenReturn(true);
            when(tokenProvider.isInvalidated(token)).thenReturn(false);
            PATAuthSource authSource = new PATAuthSource(token);
            PATAuthSourceService patAuthSourceService = new PATAuthSourceService(authenticationService, tokenProvider, tokenCreationService);
            assertTrue(patAuthSourceService.isValid(authSource));
        }

    }

    @Nested
    class GivenInvalidTokenTest {
        @Test
        void whenExceptionIsThrown_thenReturnTokenInvalid() {
            String serviceId = "gateway";
            when(context.get(SERVICE_ID_KEY)).thenReturn(serviceId);
            when(tokenProvider.isValidForScopes(token, serviceId)).thenThrow(new RuntimeException());
            PATAuthSource authSource = new PATAuthSource(token);
            PATAuthSourceService patAuthSourceService = new PATAuthSourceService(authenticationService, tokenProvider, tokenCreationService);
            assertFalse(patAuthSourceService.isValid(authSource));
        }

        @Test
        void whenTokenIsExpired_thenThrow() {
            HttpServletRequest request = new MockHttpServletRequest();
            when(authenticationService.getJwtTokenFromRequest(request)).thenReturn(Optional.of(token));
            when(authenticationService.getTokenOrigin(token)).thenThrow(new TokenExpireException("token expired"));
            PATAuthSourceService patAuthSourceService = new PATAuthSourceService(authenticationService, tokenProvider, tokenCreationService);

            assertThrows(TokenExpireException.class, () -> patAuthSourceService.getToken(request));
            verify(authenticationService, times(1)).getTokenOrigin(token);
        }

        @Test
        void whenTokenIsNotValid_thenThrow() {
            HttpServletRequest request = new MockHttpServletRequest();
            when(authenticationService.getJwtTokenFromRequest(request)).thenReturn(Optional.of(token));
            when(authenticationService.getTokenOrigin(token)).thenThrow(new TokenNotValidException("token not valid"));
            PATAuthSourceService patAuthSourceService = new PATAuthSourceService(authenticationService, tokenProvider, tokenCreationService);

            assertThrows(TokenNotValidException.class, () -> patAuthSourceService.getToken(request));
            verify(authenticationService, times(1)).getTokenOrigin(token);
        }

    }


    @Nested
    class GivenDifferentAuthSourcesTest {
        @Test
        void givenPATAuthSource_thenReturnCorrectUserInfo() {
            PATAuthSource authSource = new PATAuthSource(token);
            QueryResponse response = new QueryResponse(null, "user", new Date(), new Date(), "issuer", null, QueryResponse.Source.ZOWE_PAT);
            when(authenticationService.parseJwtWithSignature(token)).thenReturn(response);
            AuthSource.Parsed parsedSource = patAuthSourceService.parse(authSource);
            assertEquals(response.getUserId(), parsedSource.getUserId());
        }

        @Test
        void givenJWTAuthSource_thenReturnNull() {
            JwtAuthSource authSource = new JwtAuthSource(token);
            AuthSource.Parsed parsedSource = patAuthSourceService.parse(authSource);
            assertNull(parsedSource);
        }

        @Test
        void givenValidAuthSource_thenReturnLTPAToken() {
            String ltpa = "ltpa";
            PATAuthSource authSource = new PATAuthSource(token);
            QueryResponse response = new QueryResponse(null, "user", new Date(), new Date(), "issuer", null, QueryResponse.Source.ZOWE);
            when(authenticationService.parseJwtWithSignature(token)).thenReturn(response);
            when(tokenCreationService.createJwtTokenWithoutCredentials(response.getUserId())).thenReturn(token);
            when(authenticationService.getTokenOrigin(token)).thenReturn(AuthSource.Origin.ZOWE);
            when(authenticationService.getLtpaToken(token)).thenReturn(ltpa);
            String ltpaResult = patAuthSourceService.getLtpaToken(authSource);
            assertEquals(ltpa, ltpaResult);
        }

        @Test
        void givenValidAuthSource_thenReturnJWT() {
            PATAuthSource authSource = new PATAuthSource(token);
            QueryResponse response = new QueryResponse(null, "user", new Date(), new Date(), "issuer", null, QueryResponse.Source.ZOWE_PAT);
            when(authenticationService.parseJwtWithSignature(token)).thenReturn(response);
            when(tokenCreationService.createJwtTokenWithoutCredentials(response.getUserId())).thenReturn(token);
            String jwt = patAuthSourceService.getJWT(authSource);
            assertEquals(token, jwt);
        }
    }


}
