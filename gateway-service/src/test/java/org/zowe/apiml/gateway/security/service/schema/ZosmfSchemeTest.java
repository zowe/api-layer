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
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.HttpGet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.zowe.apiml.gateway.security.service.schema.ZosmfScheme.ZosmfCommand.COOKIE_HEADER;

@ExtendWith(MockitoExtension.class)
public class ZosmfSchemeTest extends CleanCurrentRequestContextTest {

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private AuthConfigurationProperties authConfigurationProperties;

    @InjectMocks
    private ZosmfScheme zosmfScheme;

    @Test
    public void givenNoToken_whenCreateCommand_thenDontAddZuulHeader() {
        Calendar calendar = Calendar.getInstance();
        Authentication authentication = new Authentication(AuthenticationScheme.ZOSMF, null);
        QueryResponse queryResponse = new QueryResponse("domain", "username", calendar.getTime(), calendar.getTime(), QueryResponse.Source.ZOWE);

        RequestContext requestContext = spy(new RequestContext());
        RequestContext.testSetCurrentContext(requestContext);

        HttpServletRequest request = new MockHttpServletRequest();
        requestContext.setRequest(request);

        when(authenticationService.getJwtTokenFromRequest(request)).thenReturn(Optional.empty());
        zosmfScheme.createCommand(authentication, () -> queryResponse).apply(null);
        verify(requestContext, never()).addZuulRequestHeader(anyString(), anyString());

    }

    @Test
    public void givenRequestWithNoCookie_whenCreateCommand_thenAddOnlyLtpaCookie() {
        Calendar calendar = Calendar.getInstance();
        Authentication authentication = new Authentication(AuthenticationScheme.ZOSMF, null);
        QueryResponse queryResponse = new QueryResponse("domain", "username", calendar.getTime(), calendar.getTime(), QueryResponse.Source.ZOWE);

        RequestContext requestContext = spy(new RequestContext());
        RequestContext.testSetCurrentContext(requestContext);

        HttpServletRequest request = new MockHttpServletRequest();
        requestContext.setRequest(request);

        when(authenticationService.getJwtTokenFromRequest(request)).thenReturn(Optional.of("jwtToken1"));
        when(authenticationService.getLtpaTokenWithValidation("jwtToken1")).thenReturn("ltpa1");
        when(authenticationService.parseJwtToken("jwtToken1")).thenReturn(queryResponse);
        requestContext.getZuulRequestHeaders().put(COOKIE_HEADER, null);
        zosmfScheme.createCommand(authentication, () -> queryResponse).apply(null);
        assertEquals("LtpaToken2=ltpa1", requestContext.getZuulRequestHeaders().get(COOKIE_HEADER));

    }

    @Test
    public void givenRequestWithSetCookie_whenCreateCommand_thenAppendSetCookie() {
        Calendar calendar = Calendar.getInstance();
        Authentication authentication = new Authentication(AuthenticationScheme.ZOSMF, null);
        QueryResponse queryResponse = new QueryResponse("domain", "username", calendar.getTime(), calendar.getTime(), QueryResponse.Source.ZOWE);

        RequestContext requestContext = spy(new RequestContext());
        RequestContext.testSetCurrentContext(requestContext);

        HttpServletRequest request = new MockHttpServletRequest();
        requestContext.setRequest(request);

        when(authenticationService.getJwtTokenFromRequest(request)).thenReturn(Optional.of("jwtToken2"));
        when(authenticationService.getLtpaTokenWithValidation("jwtToken2")).thenReturn("ltpa2");
        when(authenticationService.parseJwtToken("jwtToken2")).thenReturn(queryResponse);
        requestContext.getZuulRequestHeaders().put(COOKIE_HEADER, "cookie1=1");
        zosmfScheme.createCommand(authentication, () -> queryResponse).apply(null);
        assertEquals("cookie1=1;LtpaToken2=ltpa2", requestContext.getZuulRequestHeaders().get(COOKIE_HEADER));

    }

    @Test
    public void givenNotValidToken_whenCreateCommand_thenThrowTokenNotValidException() {
        Calendar calendar = Calendar.getInstance();
        Authentication authentication = new Authentication(AuthenticationScheme.ZOSMF, null);
        QueryResponse queryResponse = new QueryResponse("domain", "username", calendar.getTime(), calendar.getTime(), QueryResponse.Source.ZOWE);

        RequestContext requestContext = spy(new RequestContext());
        RequestContext.testSetCurrentContext(requestContext);

        HttpServletRequest request = new MockHttpServletRequest();
        requestContext.setRequest(request);

        when(authenticationService.getJwtTokenFromRequest(request)).thenReturn(Optional.of("jwtToken3"));
        when(authenticationService.getLtpaTokenWithValidation("jwtToken3")).thenThrow(new TokenNotValidException("Token is not valid"));
        when(authenticationService.parseJwtToken("jwtToken3")).thenReturn(queryResponse);

        Exception exception = assertThrows(TokenNotValidException.class,
            () -> zosmfScheme.createCommand(authentication, () -> queryResponse).apply(null),
            " Token is not valid");
        assertEquals("Token is not valid", exception.getMessage());

    }

    @Test
    public void givenExpiredToken_whenCreateCommand_thenThrowJwtTokenException() {
        Calendar calendar = Calendar.getInstance();
        Authentication authentication = new Authentication(AuthenticationScheme.ZOSMF, null);
        QueryResponse queryResponse = new QueryResponse("domain", "username", calendar.getTime(), calendar.getTime(), QueryResponse.Source.ZOWE);

        RequestContext requestContext = spy(new RequestContext());
        RequestContext.testSetCurrentContext(requestContext);

        HttpServletRequest request = new MockHttpServletRequest();
        requestContext.setRequest(request);

        when(authenticationService.getJwtTokenFromRequest(request)).thenReturn(Optional.of("jwtToken3"));
        when(authenticationService.getLtpaTokenWithValidation("jwtToken3")).thenThrow(new JwtException("Token is expired"));
        when(authenticationService.parseJwtToken("jwtToken3")).thenReturn(queryResponse);

        Exception exception = assertThrows(JwtException.class,
            () -> zosmfScheme.createCommand(authentication, () -> queryResponse).apply(null),
            " Token is expired");
        assertEquals("Token is expired", exception.getMessage());

    }

    @Test
    public void givenZosmfScheme_whenGetScheme_thenReturnScheme() {
        ZosmfScheme scheme = new ZosmfScheme(authenticationService, authConfigurationProperties);
        assertEquals(AuthenticationScheme.ZOSMF, scheme.getScheme());
    }

    @Test
    public void givenNoToken_whenCreateCommand_thenTestCommandExpiration() {
        ZosmfScheme scheme = new ZosmfScheme(authenticationService, authConfigurationProperties);

        AuthenticationCommand command;

        command = scheme.createCommand(null, () -> null);
        assertNull(ReflectionTestUtils.getField(command, "expireAt"));
        assertFalse(command.isExpired());
    }

    @Test
    public void givenTokenWithoutExpiration_whenCreateCommand_thenTestCommandExpiration() {
        ZosmfScheme scheme = new ZosmfScheme(authenticationService, authConfigurationProperties);

        AuthenticationCommand command;

        QueryResponse queryResponse = new QueryResponse();

        command = scheme.createCommand(null, () -> queryResponse);
        assertNull(ReflectionTestUtils.getField(command, "expireAt"));
        assertFalse(command.isExpired());
    }

    @Test
    public void givenTokenWithExpiration_whenCreateCommand_thenTestCommandExpiration() {
        ZosmfScheme scheme = new ZosmfScheme(authenticationService, authConfigurationProperties);

        AuthenticationCommand command;

        QueryResponse queryResponse = new QueryResponse();

        queryResponse.setExpiration(new Date(123L));
        command = scheme.createCommand(null, () -> queryResponse);
        assertNotNull(ReflectionTestUtils.getField(command, "expireAt"));
        assertEquals(123L, ReflectionTestUtils.getField(command, "expireAt"));
        assertTrue(command.isExpired());
    }

    @Test
    public void givenTokenExpiredOneSecAgo_whenCreateCommand_thenTestCommandExpiration() {
        ZosmfScheme scheme = new ZosmfScheme(authenticationService, authConfigurationProperties);

        AuthenticationCommand command;

        QueryResponse queryResponse = new QueryResponse();

        Calendar c = Calendar.getInstance();
        c.add(Calendar.SECOND, -1);
        queryResponse.setExpiration(c.getTime());
        command = scheme.createCommand(null, () -> queryResponse);
        assertTrue(command.isExpired());
    }

    @Test
    public void givenTokenThatWillExpireInOneSec_whenCreateCommand_thenTestCommandExpiration() {
        ZosmfScheme scheme = new ZosmfScheme(authenticationService, authConfigurationProperties);

        AuthenticationCommand command;

        QueryResponse queryResponse = new QueryResponse();

        Calendar c = Calendar.getInstance();
        c.add(Calendar.SECOND, 2);
        queryResponse.setExpiration(c.getTime());
        command = scheme.createCommand(null, () -> queryResponse);
        assertFalse(command.isExpired());
    }

    @Test
    public void givenZosmfToken_whenCreateCommand_thenTestJwtToken() {
        AuthConfigurationProperties.CookieProperties cookieProperties = mock(AuthConfigurationProperties.CookieProperties.class);
        when(cookieProperties.getCookieName()).thenReturn("apimlAuthenticationToken");
        when(authConfigurationProperties.getCookieProperties()).thenReturn(cookieProperties);

        ZosmfScheme scheme = new ZosmfScheme(authenticationService, authConfigurationProperties);
        QueryResponse queryResponse = new QueryResponse("domain", "username", new Date(), new Date(), QueryResponse.Source.ZOSMF);
        when(authenticationService.getJwtTokenFromRequest(any())).thenReturn(Optional.of("jwtTokenZosmf"));
        when(authenticationService.parseJwtToken("jwtTokenZosmf")).thenReturn(queryResponse);

        AuthenticationCommand command = scheme.createCommand(new Authentication(AuthenticationScheme.ZOSMF, null), () -> queryResponse);

        command.apply(null);
        verify(authenticationService, times(1)).getJwtTokenFromRequest(any());
        verify(authenticationService, times(1)).parseJwtToken("jwtTokenZosmf");
        verify(authenticationService, never()).getLtpaTokenWithValidation("jwtTokenZosmf");
    }

    @Test
    public void givenNoJwtToken_whenCreateCommand_thenExpectValidJwtRequired() {
        ZosmfScheme scheme = new ZosmfScheme(authenticationService, authConfigurationProperties);
        AuthenticationCommand command = scheme.createCommand(new Authentication(AuthenticationScheme.ZOSMF, null), () -> null);

        assertTrue(command.isRequiredValidJwt());
    }

    @Test
    public void givenRequestWithSetCookie_whenApplyToRequest_thenAppendSetCookie() {
        Calendar calendar = Calendar.getInstance();
        QueryResponse queryResponse = new QueryResponse("domain", "username", calendar.getTime(), calendar.getTime(), QueryResponse.Source.ZOWE);

        RequestContext requestContext = spy(new RequestContext());
        RequestContext.testSetCurrentContext(requestContext);

        HttpRequest httpRequest = new HttpGet("/test/request");
        httpRequest.setHeader(COOKIE_HEADER, "cookie1=1");
        Authentication authentication = new Authentication(AuthenticationScheme.ZOSMF, null);
        HttpServletRequest request = new MockHttpServletRequest();
        requestContext.setRequest(request);

        when(authenticationService.getJwtTokenFromRequest(request)).thenReturn(Optional.of("jwtToken2"));
        when(authenticationService.getLtpaTokenWithValidation("jwtToken2")).thenReturn("ltpa2");
        when(authenticationService.parseJwtToken("jwtToken2")).thenReturn(queryResponse);

        zosmfScheme.createCommand(authentication, () -> queryResponse).applyToRequest(httpRequest);

        assertEquals("cookie1=1;LtpaToken2=ltpa2", httpRequest.getFirstHeader("cookie").getValue());
    }

    @Test
    public void givenRequestWithNoCookie_whenApplyToRequest_thenAppendSetCookie() {
        Calendar calendar = Calendar.getInstance();
        QueryResponse queryResponse = new QueryResponse("domain", "username", calendar.getTime(), calendar.getTime(), QueryResponse.Source.ZOWE);

        RequestContext requestContext = spy(new RequestContext());
        RequestContext.testSetCurrentContext(requestContext);

        HttpRequest httpRequest = new HttpGet("/test/request");
        Authentication authentication = new Authentication(AuthenticationScheme.ZOSMF, null);
        HttpServletRequest request = new MockHttpServletRequest();
        requestContext.setRequest(request);

        when(authenticationService.getJwtTokenFromRequest(request)).thenReturn(Optional.of("jwtToken2"));
        when(authenticationService.getLtpaTokenWithValidation("jwtToken2")).thenReturn("ltpa1");
        when(authenticationService.parseJwtToken("jwtToken2")).thenReturn(queryResponse);

        zosmfScheme.createCommand(authentication, () -> queryResponse).applyToRequest(httpRequest);

        assertEquals("LtpaToken2=ltpa1", httpRequest.getFirstHeader("cookie").getValue());
    }

    @Test
    public void givenRequest_whenApplyToRequest_thenSetAuthHeaderToNull() {
        Calendar calendar = Calendar.getInstance();
        QueryResponse queryResponse = new QueryResponse("domain", "username", calendar.getTime(), calendar.getTime(), QueryResponse.Source.ZOWE);

        RequestContext requestContext = spy(new RequestContext());
        RequestContext.testSetCurrentContext(requestContext);

        HttpRequest httpRequest = new HttpGet("/test/request");
        Authentication authentication = new Authentication(AuthenticationScheme.ZOSMF, null);
        HttpServletRequest request = new MockHttpServletRequest();
        requestContext.setRequest(request);

        when(authenticationService.getJwtTokenFromRequest(request)).thenReturn(Optional.of("jwtToken2"));
        when(authenticationService.getLtpaTokenWithValidation("jwtToken2")).thenReturn("ltpa1");
        when(authenticationService.parseJwtToken("jwtToken2")).thenReturn(queryResponse);

        zosmfScheme.createCommand(authentication, () -> queryResponse).applyToRequest(httpRequest);

        assertEquals(null, httpRequest.getFirstHeader(HttpHeaders.AUTHORIZATION).getValue());
    }

    @Test
    public void givenZosmfToken_whenApplyToRequest_thenTestJwtToken() {
        Calendar calendar = Calendar.getInstance();
        QueryResponse queryResponse = new QueryResponse("domain", "username", calendar.getTime(), calendar.getTime(), QueryResponse.Source.ZOSMF);
        AuthConfigurationProperties.CookieProperties cookieProperties = mock(AuthConfigurationProperties.CookieProperties.class);
        when(cookieProperties.getCookieName()).thenReturn("apimlAuthenticationToken");
        when(authConfigurationProperties.getCookieProperties()).thenReturn(cookieProperties);
        RequestContext requestContext = spy(new RequestContext());
        RequestContext.testSetCurrentContext(requestContext);

        HttpRequest httpRequest = new HttpGet("/test/request");
        httpRequest.setHeader(COOKIE_HEADER, "cookie1=1");
        Authentication authentication = new Authentication(AuthenticationScheme.ZOSMF, null);
        HttpServletRequest request = new MockHttpServletRequest();
        requestContext.setRequest(request);

        when(authenticationService.getJwtTokenFromRequest(request)).thenReturn(Optional.of("jwtToken2"));
        when(authenticationService.parseJwtToken("jwtToken2")).thenReturn(queryResponse);
        when(authConfigurationProperties.getCookieProperties().getCookieName()).thenReturn("apimlAuthenticationToken");
        zosmfScheme.createCommand(authentication, () -> queryResponse).applyToRequest(httpRequest);

        assertEquals("cookie1=1;jwtToken=jwtToken2", httpRequest.getFirstHeader("cookie").getValue());
    }

}
