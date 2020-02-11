/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.service.schema;

import com.netflix.zuul.context.RequestContext;
import io.jsonwebtoken.JwtException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.gateway.security.service.AuthenticationException;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.gateway.utils.CleanCurrentRequestContextTest;
import org.zowe.apiml.security.common.auth.Authentication;
import org.zowe.apiml.security.common.auth.AuthenticationScheme;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.token.QueryResponse;
import org.zowe.apiml.security.common.token.TokenNotValidException;

import javax.servlet.http.HttpServletRequest;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.zowe.apiml.gateway.security.service.schema.ZosmfScheme.ZosmfCommand.COOKIE_HEADER;

@RunWith(MockitoJUnitRunner.class)
public class ZosmfSchemeTest extends CleanCurrentRequestContextTest {

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private AuthConfigurationProperties authConfigurationProperties;

    @InjectMocks
    private ZosmfScheme zosmfScheme;

    @Before
    public void setUp() {
        AuthConfigurationProperties.CookieProperties cookieProperties = mock(AuthConfigurationProperties.CookieProperties.class);
        when(cookieProperties.getCookieName()).thenReturn("apimlAuthenticationToken");
        when(authConfigurationProperties.getCookieProperties()).thenReturn(cookieProperties);
    }

    @Test
    public void testCreateCommand() throws Exception {
        Calendar calendar = Calendar.getInstance();
        Authentication authentication = new Authentication(AuthenticationScheme.ZOSMF, null);
        QueryResponse queryResponse = new QueryResponse("domain", "username", calendar.getTime(), calendar.getTime(), QueryResponse.Source.ZOWE);

        RequestContext requestContext = spy(new RequestContext());
        RequestContext.testSetCurrentContext(requestContext);

        HttpServletRequest request = new MockHttpServletRequest();
        requestContext.setRequest(request);

        // no token exists
        when(authenticationService.getJwtTokenFromRequest(request)).thenReturn(Optional.empty());
        zosmfScheme.createCommand(authentication, queryResponse).apply(null);
        verify(requestContext, never()).addZuulRequestHeader(anyString(), anyString());

        // no cookies is set now
        reset(authenticationService);
        when(authenticationService.getJwtTokenFromRequest(request)).thenReturn(Optional.of("jwtToken1"));
        when(authenticationService.getLtpaTokenWithValidation("jwtToken1")).thenReturn("ltpa1");
        when(authenticationService.parseJwtToken("jwtToken1")).thenReturn(queryResponse);
        requestContext.getZuulRequestHeaders().put(COOKIE_HEADER, null);
        zosmfScheme.createCommand(authentication, queryResponse).apply(null);
        assertEquals("LtpaToken2=ltpa1", requestContext.getZuulRequestHeaders().get(COOKIE_HEADER));

        // a cookies is set now
        reset(authenticationService);
        when(authenticationService.getJwtTokenFromRequest(request)).thenReturn(Optional.of("jwtToken2"));
        when(authenticationService.getLtpaTokenWithValidation("jwtToken2")).thenReturn("ltpa2");
        when(authenticationService.parseJwtToken("jwtToken2")).thenReturn(queryResponse);
        requestContext.getZuulRequestHeaders().put(COOKIE_HEADER, "cookie1=1");
        zosmfScheme.createCommand(authentication, queryResponse).apply(null);
        assertEquals("cookie1=1;LtpaToken2=ltpa2", requestContext.getZuulRequestHeaders().get(COOKIE_HEADER));

        // JWT token is not valid anymore - TokenNotValidException
        try {
            reset(authenticationService);
            when(authenticationService.getJwtTokenFromRequest(request)).thenReturn(Optional.of("jwtToken3"));
            when(authenticationService.getLtpaTokenWithValidation("jwtToken3")).thenThrow(new TokenNotValidException("Token is not valid"));
            when(authenticationService.parseJwtToken("jwtToken3")).thenReturn(queryResponse);
            zosmfScheme.createCommand(authentication, queryResponse).apply(null);
            fail();
        } catch (TokenNotValidException e) {
            // exception is not handled
        }

        // JWT token is not valid anymore - JwtException
        try {
            reset(authenticationService);
            when(authenticationService.getJwtTokenFromRequest(request)).thenReturn(Optional.of("jwtToken3"));
            when(authenticationService.getLtpaTokenWithValidation("jwtToken3")).thenThrow(new JwtException("Token is expired"));
            when(authenticationService.parseJwtToken("jwtToken3")).thenReturn(queryResponse);
            zosmfScheme.createCommand(authentication, queryResponse).apply(null);
            fail();
        } catch (JwtException e) {
            // exception is not handled
        }
    }

    @Test
    public void testScheme() {
        ZosmfScheme scheme = new ZosmfScheme(authenticationService, authConfigurationProperties);
        assertEquals(AuthenticationScheme.ZOSMF, scheme.getScheme());
    }

    @Test
    public void testExpiration() {
        ZosmfScheme scheme = new ZosmfScheme(authenticationService, authConfigurationProperties);

        AuthenticationCommand command;
        QueryResponse queryResponse = new QueryResponse();

        // no JWT token
        command = scheme.createCommand(null, null);
        assertNull(ReflectionTestUtils.getField(command, "expireAt"));
        assertFalse(command.isExpired());

        // token without expiration
        command = scheme.createCommand(null, queryResponse);
        assertNull(ReflectionTestUtils.getField(command, "expireAt"));
        assertFalse(command.isExpired());

        // token with expiration
        queryResponse.setExpiration(new Date(123L));
        command = scheme.createCommand(null, queryResponse);
        assertNotNull(ReflectionTestUtils.getField(command, "expireAt"));
        assertEquals(123L, ReflectionTestUtils.getField(command, "expireAt"));
        assertTrue(command.isExpired());

        // expired 1 sec ago
        Calendar c = Calendar.getInstance();
        c.add(Calendar.SECOND, -1);
        queryResponse.setExpiration(c.getTime());
        command = scheme.createCommand(null, queryResponse);
        assertTrue(command.isExpired());

        // expired in 1 secs
        c.add(Calendar.SECOND, 2);
        queryResponse.setExpiration(c.getTime());
        command = scheme.createCommand(null, queryResponse);
        assertFalse(command.isExpired());
    }

    @Test
    public void testZosmfToken() throws AuthenticationException {
        ZosmfScheme scheme = new ZosmfScheme(authenticationService, authConfigurationProperties);
        QueryResponse queryResponse = new QueryResponse("domain", "username", new Date(), new Date(), QueryResponse.Source.ZOSMF);
        when(authenticationService.getJwtTokenFromRequest(any())).thenReturn(Optional.of("jwtTokenZosmf"));
        when(authenticationService.parseJwtToken("jwtTokenZosmf")).thenReturn(queryResponse);

        AuthenticationCommand command = scheme.createCommand(new Authentication(AuthenticationScheme.ZOSMF, null), queryResponse);

        command.apply(null);
        verify(authenticationService, times(1)).getJwtTokenFromRequest(any());
        verify(authenticationService, times(1)).parseJwtToken("jwtTokenZosmf");
        verify(authenticationService, never()).getLtpaTokenWithValidation("jwtTokenZosmf");
    }

}
