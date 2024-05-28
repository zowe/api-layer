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

import com.netflix.zuul.context.RequestContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.zowe.apiml.zaas.security.mapping.AuthenticationMapper;
import org.zowe.apiml.zaas.security.service.AuthenticationService;
import org.zowe.apiml.zaas.security.service.TokenCreationService;
import org.zowe.apiml.security.common.token.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OIDCAuthSourceServiceTest {
    private RequestContext context;
    private TokenCreationService tokenCreationService;
    private OIDCAuthSourceService service;
    private AuthenticationService authenticationService;
    private OIDCProvider provider;
    private AuthenticationMapper mapper;
    private static final String TOKEN = "token";
    private static final String ISSUER = "issuer";
    private static final String DISTRIB_USER = "pc.user@acme.com";
    private static final String MF_USER = "MF_USER";

    @BeforeEach
    void setup() {
        authenticationService = mock(AuthenticationService.class);
        tokenCreationService = mock(TokenCreationService.class);
        provider = mock(OIDCProvider.class);
        mapper = mock(AuthenticationMapper.class);
        service = new OIDCAuthSourceService(mapper, authenticationService, provider, tokenCreationService);
        context = mock(RequestContext.class);
        RequestContext.testSetCurrentContext(context);
    }

    @AfterAll
    static void cleanUp() {
        RequestContext.testSetCurrentContext(null);
    }

    @Test
    void returnOIDCSourceMapper() {
        assertTrue(service.getMapper().apply(TOKEN) instanceof OIDCAuthSource);
    }

    @Test
    void returnLogger() {
        assertNotNull(service.getLogger());
    }

    @Nested
    class GivenValidTokenTest {
        @Test
        void givenOidcTokenInRequestContext_thenReturnTheToken() {
            HttpServletRequest request = new MockHttpServletRequest();
            when(authenticationService.getJwtTokenFromRequest(request)).thenReturn(Optional.of(TOKEN));
            when(authenticationService.getTokenOrigin(TOKEN)).thenReturn(AuthSource.Origin.OIDC);
            Optional<String> tokenResult = service.getToken(request);
            assertTrue(tokenResult.isPresent());
            assertEquals(TOKEN, tokenResult.get());
        }

        @Test
        void givenPatTokenInRequestContext_thenReturnEmpty() {
            HttpServletRequest request = new MockHttpServletRequest();
            when(authenticationService.getJwtTokenFromRequest(request)).thenReturn(Optional.of(TOKEN));
            when(authenticationService.getTokenOrigin(TOKEN)).thenReturn(AuthSource.Origin.ZOWE_PAT);
            Optional<String> tokenResult = service.getToken(request);
            assertFalse(tokenResult.isPresent());
        }

        @Test
        void givenTokenInAuthSource_thenReturnValid() {
            OIDCAuthSource authSource = mockValidAuthSource();
            assertTrue(service.isValid(authSource));
        }

        @Test
        void whenParse_thenCorrect() {
            OIDCAuthSource authSource = mockValidAuthSource();
            when(mapper.mapToMainframeUserId(authSource)).thenReturn(MF_USER);
            AuthSource.Parsed parsedSource = service.parse(authSource);

            verify(mapper, times(1)).mapToMainframeUserId(authSource);
            assertEquals(MF_USER, parsedSource.getUserId());
        }

        @Test
        void givenNoMapping_whenParse_thenThrowException() {
            OIDCAuthSource authSource = mockValidAuthSource();
            when(mapper.mapToMainframeUserId(authSource)).thenReturn(null);

            assertThrows(NoMainframeIdentityException.class, () -> {
                service.parse(authSource);
            });
        }

        @Test
        void givenValidAuthSource_thenReturnLTPAToken() {
            OIDCAuthSource authSource = mockValidAuthSource();
            String expectedToken = "ltpa-token";
            when(mapper.mapToMainframeUserId(any())).thenReturn(MF_USER);
            String zoweToken = "zowe-token";
            when(tokenCreationService.createJwtTokenWithoutCredentials(MF_USER)).thenReturn(zoweToken);
            when(authenticationService.getTokenOrigin(zoweToken)).thenReturn(AuthSource.Origin.ZOWE);
            when(authenticationService.getLtpaToken(zoweToken)).thenReturn(expectedToken);

            String ltpaResult = service.getLtpaToken(authSource);
            assertEquals(expectedToken, ltpaResult);
        }

        @Test
        void givenValidAuthSource_thenReturnJWT() {
            OIDCAuthSource authSource = mockValidAuthSource();
            when(mapper.mapToMainframeUserId(any())).thenReturn(MF_USER);
            String expectedToken = "jwt-token";
            when(tokenCreationService.createJwtTokenWithoutCredentials(MF_USER)).thenReturn(expectedToken);
            String jwtResult = service.getJWT(authSource);
            assertEquals(expectedToken, jwtResult);
        }
    }

    @Nested
    class GivenDifferentAuthSourcesTest {

        @Test
        void givenJWTAuthSourceWhenValidating_thenReturnFalse() {
            JwtAuthSource authSource = new JwtAuthSource(TOKEN);
            boolean isValid = service.isValid(authSource);
            assertFalse(isValid);
        }

        @Test
        void givenJWTAuthSource_thenReturnNull() {
            JwtAuthSource authSource = new JwtAuthSource(TOKEN);
            AuthSource.Parsed parsedSource = service.parse(authSource);
            assertNull(parsedSource);
        }
    }

    @Nested
    class GivenInvalidTokenTest {
        @Test
        void whenTokenIsNull_thenReturnTokenInvalid() {
            OIDCAuthSource authSource = new OIDCAuthSource(null);
            assertFalse(service.isValid(authSource));
        }

        @Test
        void whenTokenIsEmpty_thenReturnTokenInvalid() {
            OIDCAuthSource authSource = new OIDCAuthSource("");
            assertFalse(service.isValid(authSource));
        }

        @Test
        void whenIsInvalid_thenReturnTokenInvalid() {
            OIDCAuthSource authSource = new OIDCAuthSource(TOKEN);
            when(provider.isValid(TOKEN)).thenReturn(false);
            assertFalse(service.isValid(authSource));
        }

        @Test
        void whenParse_thenReturnNull() {
            OIDCAuthSource authSource = new OIDCAuthSource(TOKEN);
            when(provider.isValid(TOKEN)).thenReturn(false);
            AuthSource.Parsed parsedSource = service.parse(authSource);

            verify(mapper, times(0)).mapToMainframeUserId(authSource);
            assertNull(parsedSource);
        }

        @Test
        void whenTokenIsExpired_thenThrow() {
            HttpServletRequest request = new MockHttpServletRequest();
            when(authenticationService.getJwtTokenFromRequest(request)).thenReturn(Optional.of(TOKEN));
            when(authenticationService.getTokenOrigin(TOKEN)).thenThrow(new TokenExpireException("token expired"));

            assertThrows(TokenExpireException.class, () -> service.getToken(request));
            verify(authenticationService, times(1)).getTokenOrigin(TOKEN);
        }

        @Test
        void whenTokenIsNotValid_thenThrow() {
            HttpServletRequest request = new MockHttpServletRequest();
            when(authenticationService.getJwtTokenFromRequest(request)).thenReturn(Optional.of(TOKEN));
            when(authenticationService.getTokenOrigin(TOKEN)).thenThrow(new TokenNotValidException("token not valid"));

            assertThrows(TokenNotValidException.class, () -> service.getToken(request));
            verify(authenticationService, times(1)).getTokenOrigin(TOKEN);
        }
    }

    private OIDCAuthSource mockValidAuthSource() {
        QueryResponse tokenResponse = new QueryResponse("domain", DISTRIB_USER, new Date(), new Date(), ISSUER, Collections.emptyList(), QueryResponse.Source.OIDC);
        when(authenticationService.parseJwtToken(TOKEN)).thenReturn(tokenResponse);
        when(provider.isValid(TOKEN)).thenReturn(true);
        return new OIDCAuthSource(TOKEN);
    }
}
