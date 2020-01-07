package com.ca.mfaas.gateway.security.service.schema;/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import com.ca.apiml.security.common.auth.Authentication;
import com.ca.apiml.security.common.auth.AuthenticationScheme;
import com.ca.apiml.security.common.token.QueryResponse;
import com.ca.apiml.security.common.token.TokenNotValidException;
import com.ca.mfaas.gateway.security.service.AuthenticationService;
import com.netflix.zuul.context.RequestContext;
import io.jsonwebtoken.JwtException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import java.util.Calendar;
import java.util.Optional;

import static com.ca.mfaas.gateway.security.service.schema.ZosmfScheme.ZosmfCommand.COOKIE_HEADER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ZosmfSchemeTest {

    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private ZosmfScheme zosmfScheme;

    @Test
    public void testCreateCommand() throws Exception {
        Calendar calendar = Calendar.getInstance();
        Authentication authentication = new Authentication(AuthenticationScheme.ZOSMF, null);
        QueryResponse queryResponse = new QueryResponse("domain", "username", calendar.getTime(), calendar.getTime());

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
        when(authenticationService.getLtpaTokenFromJwtToken("jwtToken1")).thenReturn("ltpa1");
        requestContext.getZuulRequestHeaders().put(COOKIE_HEADER, null);
        zosmfScheme.createCommand(authentication, queryResponse).apply(null);
        assertEquals("ltpa1", requestContext.getZuulRequestHeaders().get(COOKIE_HEADER));

        // a cookies is set now
        reset(authenticationService);
        when(authenticationService.getJwtTokenFromRequest(request)).thenReturn(Optional.of("jwtToken2"));
        when(authenticationService.getLtpaTokenFromJwtToken("jwtToken2")).thenReturn("ltpa2");
        requestContext.getZuulRequestHeaders().put(COOKIE_HEADER, "cookie1=1");
        zosmfScheme.createCommand(authentication, queryResponse).apply(null);
        assertEquals("cookie1=1; ltpa2", requestContext.getZuulRequestHeaders().get(COOKIE_HEADER));

        // JWT token is not valid anymore - TokenNotValidException
        try {
            reset(authenticationService);
            when(authenticationService.getJwtTokenFromRequest(request)).thenReturn(Optional.of("jwtToken3"));
            when(authenticationService.getLtpaTokenFromJwtToken("jwtToken3")).thenThrow(new TokenNotValidException("Token is not valid"));
            zosmfScheme.createCommand(authentication, queryResponse).apply(null);
            fail();
        } catch (TokenNotValidException e) {
            // exception is not handled
        }

        // JWT token is not valid anymore - JwtException
        try {
            reset(authenticationService);
            when(authenticationService.getJwtTokenFromRequest(request)).thenReturn(Optional.of("jwtToken3"));
            when(authenticationService.getLtpaTokenFromJwtToken("jwtToken3")).thenThrow(new JwtException("Token is expired"));
            zosmfScheme.createCommand(authentication, queryResponse).apply(null);
            fail();
        } catch (JwtException e) {
            // exception is not handled
        }
    }

}
