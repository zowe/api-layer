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
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.gateway.security.service.TokenCreationService;
import org.zowe.apiml.security.common.token.AccessTokenProvider;
import org.zowe.apiml.security.common.token.QueryResponse;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVICE_ID_KEY;

class PATAuthSourceServiceTest {

    AuthenticationService authenticationService;
    AccessTokenProvider tokenProvider;
    TokenCreationService tokenCreationService;
    PATAuthSourceService patAuthSourceService;
    RequestContext context;

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

    @Test
    void givenTokenInRequestContext_thenReturnTheToken() {
        String token = "token";
        HttpServletRequest request = new MockHttpServletRequest();
        when(context.getRequest()).thenReturn(request);
        when(authenticationService.getPATFromRequest(request)).thenReturn(Optional.of(token));
        assertEquals(token, patAuthSourceService.getToken(context).get());
    }

    @Test
    void givenValidTokenInAuthSource_thenReturnValid() {
        String token = "token";
        String serviceId = "gateway";
        when(context.get(SERVICE_ID_KEY)).thenReturn(serviceId);
        when(tokenProvider.isValidForScopes(token, serviceId)).thenReturn(true);
        when(tokenProvider.isInvalidated(token)).thenReturn(false);
        PATAuthSource authSource = new PATAuthSource(token);
        PATAuthSourceService patAuthSourceService = new PATAuthSourceService(authenticationService, tokenProvider, tokenCreationService);
        assertTrue(patAuthSourceService.isValid(authSource));
    }

    @Test
    void whenExceptionIsThrown_thenReturnTokenInvalid() {
        String token = "token";
        String serviceId = "gateway";
        when(context.get(SERVICE_ID_KEY)).thenReturn(serviceId);
        when(tokenProvider.isValidForScopes(token, serviceId)).thenThrow(new RuntimeException());
        PATAuthSource authSource = new PATAuthSource(token);
        PATAuthSourceService patAuthSourceService = new PATAuthSourceService(authenticationService, tokenProvider, tokenCreationService);
        assertFalse(patAuthSourceService.isValid(authSource));
    }

    @Test
    void givenPATAuthSource_thenReturnCorrectUserInfo() {
        String token = "token";
        PATAuthSource authSource = new PATAuthSource(token);
        QueryResponse response = new QueryResponse(null, "user", new Date(), new Date(), null, QueryResponse.Source.ZOWE_PAT);
        when(authenticationService.parseJwtWithSignature(token)).thenReturn(response);
        AuthSource.Parsed parsedSource = patAuthSourceService.parse(authSource);
        assertEquals(response.getUserId(), parsedSource.getUserId());
    }

    @Test
    void givenJWTAuthSource_thenReturnNull() {
        String token = "token";
        JwtAuthSource authSource = new JwtAuthSource(token);
        AuthSource.Parsed parsedSource = patAuthSourceService.parse(authSource);
        assertNull(parsedSource);
    }

    @Test
    void givenValidAuthSource_thenReturnLTPAToken() {
        String token = "token";
        String ltpa = "ltpa";
        PATAuthSource authSource = new PATAuthSource(token);
        QueryResponse response = new QueryResponse(null, "user", new Date(), new Date(), null, QueryResponse.Source.ZOWE);
        when(authenticationService.parseJwtWithSignature(token)).thenReturn(response);
        when(tokenCreationService.createJwtTokenWithoutCredentials(response.getUserId())).thenReturn(token);
        when(authenticationService.parseJwtToken(token)).thenReturn(response);
        when(authenticationService.getLtpaToken(token)).thenReturn(ltpa);
        String ltpaResult = patAuthSourceService.getLtpaToken(authSource);
        assertEquals(ltpa, ltpaResult);
    }

    @Test
    void givenValidAuthSource_thenReturnJWT() {
        String token = "token";
        PATAuthSource authSource = new PATAuthSource(token);
        QueryResponse response = new QueryResponse(null, "user", new Date(), new Date(), null, QueryResponse.Source.ZOWE_PAT);
        when(authenticationService.parseJwtWithSignature(token)).thenReturn(response);
        when(tokenCreationService.createJwtTokenWithoutCredentials(response.getUserId())).thenReturn(token);
        String jwt = patAuthSourceService.getJWT(authSource);
        assertEquals(token, jwt);
    }

    @Test
    void givenCorrectToken_thenReturnPATOrigin() {
        String token = "token";
        QueryResponse response = new QueryResponse(null, "user", new Date(), new Date(), null, QueryResponse.Source.ZOWE_PAT);
        when(authenticationService.parseJwtToken(token)).thenReturn(response);
        AuthSource.Origin origin = patAuthSourceService.getTokenOrigin(token);
        assertEquals(AuthSource.Origin.valueByIssuer(QueryResponse.Source.ZOWE_PAT.name()), origin);
    }
}
