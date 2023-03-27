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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.zowe.apiml.gateway.security.mapping.AuthenticationMapper;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.gateway.security.service.TokenCreationService;
import org.zowe.apiml.security.common.token.OIDCProvider;
import org.zowe.apiml.security.common.token.QueryResponse;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class OIDCAuthSourceServiceTest {
    private RequestContext context;
    TokenCreationService tokenCreationService;
    OIDCAuthSourceService service;
    AuthenticationService authenticationService;
    OIDCProvider provider;
    AuthenticationMapper mapper;
    String token = "token";

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
        assertTrue(service.getMapper().apply("token") instanceof OIDCAuthSource);
    }

    @Nested
    class GivenValidTokenTest {
        @Test
        void givenTokenInRequestContext_thenReturnTheToken() {
            HttpServletRequest request = new MockHttpServletRequest();
            when(context.getRequest()).thenReturn(request);
            when(authenticationService.getOIDCTokenFromRequest(request)).thenReturn(Optional.of(token));
            assertEquals(token, service.getToken(context).get());
        }

        @Test
        void givenTokenInAuthSource_thenReturnValid() {
            when(provider.isValid(token)).thenReturn(true);
            OIDCAuthSource authSource = new OIDCAuthSource(token);
            assertTrue(service.isValid(authSource));
        }

        @Test
        void whenParse_thenCorrect() {
            when(provider.isValid(token)).thenReturn(true);
            OIDCAuthSource authSource = new OIDCAuthSource(token);
            QueryResponse response = new QueryResponse(null, "user", new Date(), new Date(), null, QueryResponse.Source.OIDC);
            when(authenticationService.parseJwtWithSignature(token)).thenReturn(response);
            when(mapper.mapToMainframeUserId(authSource)).thenReturn("user");
            AuthSource.Parsed parsedSource = service.parse(authSource);

            verify(mapper, times(1)).mapToMainframeUserId(authSource);
            assertEquals(response.getUserId(), parsedSource.getUserId());
        }

        @Test
        void givenValidAuthSource_thenReturnLTPAToken() {
            when(provider.isValid(token)).thenReturn(true);
            String ltpa = "ltpa";
            when(mapper.mapToMainframeUserId(any())).thenReturn("user");
            OIDCAuthSource authSource = new OIDCAuthSource(token);
            QueryResponse response = new QueryResponse(null, "user", new Date(), new Date(), null, QueryResponse.Source.ZOWE);
            when(authenticationService.parseJwtWithSignature(token)).thenReturn(response);
            when(tokenCreationService.createJwtTokenWithoutCredentials(response.getUserId())).thenReturn(token);
            when(authenticationService.parseJwtToken(token)).thenReturn(response);
            when(authenticationService.getLtpaToken(token)).thenReturn(ltpa);
            String ltpaResult = service.getLtpaToken(authSource);
            assertEquals(ltpa, ltpaResult);
        }

        @Test
        void givenValidAuthSource_thenReturnJWT() {
            when(provider.isValid(token)).thenReturn(true);
            when(mapper.mapToMainframeUserId(any())).thenReturn("user");
            OIDCAuthSource authSource = new OIDCAuthSource(token);
            QueryResponse response = new QueryResponse(null, "user", new Date(), new Date(), null, QueryResponse.Source.OIDC);
            when(authenticationService.parseJwtWithSignature(token)).thenReturn(response);
            when(tokenCreationService.createJwtTokenWithoutCredentials(response.getUserId())).thenReturn(token);
            String jwt = service.getJWT(authSource);
            assertEquals(token, jwt);
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
    }
}
