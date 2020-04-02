package org.zowe.apiml.gateway.filters.pre;/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import com.netflix.zuul.context.RequestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import org.zowe.apiml.security.common.token.TokenExpireException;
import org.zowe.apiml.security.common.token.TokenNotValidException;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class JwtValidatorFilterTest {

    private static final String USERNAME = "username";
    private static final String VALID_JWT = "validJwtToken";
    private static final String INVALID_JWT = "invalidJwtToken";

    private AuthenticationService authenticationService;
    private JwtValidatorFilter jwtValidatorFilter;
    private RequestContext requestContext;
    private AuthConfigurationProperties authConfigurationProperties;

    @BeforeEach
    public void initTest() {
        authenticationService = mock(AuthenticationService.class);
        authConfigurationProperties = mock(AuthConfigurationProperties.class);
        jwtValidatorFilter = new JwtValidatorFilter(authenticationService, authConfigurationProperties);

        when(authenticationService.validateJwtToken(VALID_JWT))
            .thenReturn(TokenAuthentication.createAuthenticated(USERNAME, VALID_JWT));
        when(authenticationService.validateJwtToken(INVALID_JWT))
            .thenReturn(new TokenAuthentication(USERNAME, INVALID_JWT));

        requestContext = mock(RequestContext.class);
        RequestContext.testSetCurrentContext(requestContext);
    }

    @AfterEach
    public void tearDown() {
        RequestContext.testSetCurrentContext(null);
    }

    @Test
    public void givenValidToken_thenSuccess_whenVerifyAccess() {
        when(authenticationService.getJwtTokenFromRequest(any()))
            .thenReturn(Optional.of(VALID_JWT));

        jwtValidatorFilter.run();

        verify(requestContext, never()).setSendZuulResponse(anyBoolean());
        verify(requestContext, never()).setResponseStatusCode(anyInt());
    }

    @Test
    public void givenInvalidToken_thenUnAthorized_whenVerifyAccess() {
        when(authenticationService.getJwtTokenFromRequest(any()))
            .thenReturn(Optional.of(INVALID_JWT));

        jwtValidatorFilter.run();

        verify(requestContext, times(1)).setSendZuulResponse(false);
        verify(requestContext, times(1)).setResponseStatusCode(401);
    }

    @Test
    public void notGivenToken_thenSuccess_whenVerifyAccess() {
        when(authenticationService.getJwtTokenFromRequest(any()))
            .thenReturn(Optional.empty());

        jwtValidatorFilter.run();

        verify(requestContext, never()).setSendZuulResponse(anyBoolean());
        verify(requestContext, never()).setResponseStatusCode(anyInt());
    }

    @Test
    public void testFilterConfiguration() {
        assertEquals("pre", jwtValidatorFilter.filterType());
        assertTrue(jwtValidatorFilter.shouldFilter());
        assertTrue(jwtValidatorFilter.filterOrder() < 0);
    }

    private HttpServletRequest initTestExpiredToken() {
        when(authenticationService.getJwtTokenFromRequest(any()))
            .thenReturn(Optional.of(VALID_JWT));
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(requestContext.getRequest()).thenReturn(request);
        when(requestContext.getZuulRequestHeaders()).thenReturn(new HashMap<>());
        return request;
    }

    @Test
    public void givenExpiredToken_thenRemovedToken_whenNoCookieSet() {
        HttpServletRequest request = initTestExpiredToken();
        when(authenticationService.validateJwtToken(anyString()))
            .thenThrow(new TokenExpireException("Token expired."));

        jwtValidatorFilter.run();

        verify(requestContext, never()).setSendZuulResponse(false);
        verify(requestContext, never()).setResponseStatusCode(401);

        assertTrue(requestContext.getZuulRequestHeaders().containsKey("Authorization"));
        assertNull(requestContext.getZuulRequestHeaders().get("Authorization"));
        assertNull(requestContext.getZuulRequestHeaders().get("cookie"));
    }

    @Test
    public void givenExpiredToken_thenRemovedToken_whenCookieSet() {
        HttpServletRequest request = initTestExpiredToken();
        when(authenticationService.validateJwtToken(anyString()))
            .thenThrow(new TokenExpireException("Token expired."));
        when(request.getHeader("cookie")).thenReturn(";;apimlAuthenticationToken=xyz;;");
        AuthConfigurationProperties.CookieProperties cp = new AuthConfigurationProperties.CookieProperties();
        cp.setCookieName("apimlAuthenticationToken");
        when(authConfigurationProperties.getCookieProperties()).thenReturn(cp);


        jwtValidatorFilter.run();

        verify(requestContext, never()).setSendZuulResponse(false);
        verify(requestContext, never()).setResponseStatusCode(401);

        assertTrue(requestContext.getZuulRequestHeaders().containsKey("Authorization"));
        assertNull(requestContext.getZuulRequestHeaders().get("Authorization"));
        assertNull(requestContext.getZuulRequestHeaders().get("cookie"));
    }

    @Test
    public void givenUnparseableToken_thenRemovedToken() {
        HttpServletRequest request = initTestExpiredToken();
        when(authenticationService.validateJwtToken(anyString()))
            .thenThrow(new TokenNotValidException("Unknown token type."));

        jwtValidatorFilter.run();

        verify(requestContext, times(1)).setSendZuulResponse(false);
        verify(requestContext, times(1)).setResponseStatusCode(401);
    }

    @Test
    public void givenInvalidToken_thenRemovedToken() {
        HttpServletRequest request = initTestExpiredToken();
        when(authenticationService.validateJwtToken(anyString()))
            .thenReturn(new TokenAuthentication("user", "token"));

        jwtValidatorFilter.run();

        verify(requestContext, times(1)).setSendZuulResponse(false);
        verify(requestContext, times(1)).setResponseStatusCode(401);
    }

}
