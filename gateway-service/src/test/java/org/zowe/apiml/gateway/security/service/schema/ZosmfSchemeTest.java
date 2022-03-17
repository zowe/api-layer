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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource.Origin;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSourceService;
import org.zowe.apiml.gateway.security.service.schema.source.JwtAuthSource;
import org.zowe.apiml.gateway.security.service.schema.source.X509AuthSource;
import org.zowe.apiml.gateway.security.service.schema.source.X509AuthSource.Parsed;
import org.zowe.apiml.gateway.utils.CleanCurrentRequestContextTest;
import org.zowe.apiml.gateway.utils.X509Utils;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.token.TokenNotValidException;

import javax.servlet.http.HttpServletRequest;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.zowe.apiml.gateway.security.service.schema.ZosmfScheme.ZosmfCommand.COOKIE_HEADER;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ZosmfSchemeTest extends CleanCurrentRequestContextTest {

    @Mock
    private AuthSourceService authSourceService;

    @Mock
    private AuthConfigurationProperties authConfigurationProperties;

    @InjectMocks
    private ZosmfScheme zosmfScheme;

    private Authentication authentication;
    private AuthSource.Parsed parsedSourceZowe;
    private AuthSource.Parsed parsedSourceZosmf;
    private AuthSource.Parsed parsedSourceX509;
    private RequestContext requestContext;
    private HttpServletRequest request;
    private ZosmfScheme scheme;

    @BeforeEach
    void prepareContextForTests() {
        Calendar calendar = Calendar.getInstance();
        authentication = new Authentication(AuthenticationScheme.ZOSMF, null);
        parsedSourceZowe = new JwtAuthSource.Parsed("username", calendar.getTime(), calendar.getTime(), Origin.ZOWE);
        parsedSourceZosmf = new JwtAuthSource.Parsed("username", calendar.getTime(), calendar.getTime(), Origin.ZOSMF);
        parsedSourceX509 = new Parsed("username", calendar.getTime(), calendar.getTime(), Origin.X509, "encoded", "distName");
        requestContext = spy(new RequestContext());
        RequestContext.testSetCurrentContext(requestContext);

        request = new MockHttpServletRequest();
        requestContext.setRequest(request);
        scheme = new ZosmfScheme(authSourceService, authConfigurationProperties);
        ReflectionTestUtils.setField(zosmfScheme, "authProvider", "zosmf");
        ReflectionTestUtils.setField(scheme, "authProvider", "zosmf");
    }

    @Test
    void givenNoToken_whenCreateCommand_thenDontAddZuulHeader() {
        when(authSourceService.getAuthSourceFromRequest()).thenReturn(Optional.empty());

        zosmfScheme.createCommand(authentication, null).apply(null);

        verify(requestContext, never()).addZuulRequestHeader(anyString(), anyString());

    }

    @Test
    void givenRequestWithNoCookie_whenCreateCommand_thenAddOnlyLtpaCookie() {
        when(authSourceService.getAuthSourceFromRequest()).thenReturn(Optional.of(new JwtAuthSource("jwtToken1")));
        when(authSourceService.getLtpaToken(new JwtAuthSource("jwtToken1"))).thenReturn("ltpa1");
        when(authSourceService.parse(new JwtAuthSource("jwtToken1"))).thenReturn(parsedSourceZowe);
        requestContext.getZuulRequestHeaders().put(COOKIE_HEADER, null);

        zosmfScheme.createCommand(authentication, new JwtAuthSource("jwtToken1")).apply(null);

        assertEquals("LtpaToken2=ltpa1", requestContext.getZuulRequestHeaders().get(COOKIE_HEADER));
    }

    @Test
    void givenRequestWithSetCookie_whenCreateCommand_thenAppendSetCookie() {
        when(authSourceService.getAuthSourceFromRequest()).thenReturn(Optional.of(new JwtAuthSource("jwtToken2")));
        when(authSourceService.getLtpaToken(new JwtAuthSource("jwtToken2"))).thenReturn("ltpa2");
        when(authSourceService.parse(new JwtAuthSource("jwtToken2"))).thenReturn(parsedSourceZowe);
        requestContext.getZuulRequestHeaders().put(COOKIE_HEADER, "cookie1=1");

        zosmfScheme.createCommand(authentication, new JwtAuthSource("jwtToken2")).apply(null);

        assertEquals("cookie1=1;LtpaToken2=ltpa2", requestContext.getZuulRequestHeaders().get(COOKIE_HEADER));
    }

    @Test
    void givenClientCertificate_thenAddZuulHeader() {
        when(authSourceService.getAuthSourceFromRequest()).thenReturn(Optional.of(new X509AuthSource(mock(
            X509Certificate.class))));
        when(authSourceService.parse(any(X509AuthSource.class))).thenReturn(parsedSourceX509);
        when(authSourceService.getJWT(any())).thenReturn("jwt");
        when(authSourceService.parse(any(JwtAuthSource.class))).thenReturn(parsedSourceZosmf);
        AuthConfigurationProperties.CookieProperties cookieProperties = mock(AuthConfigurationProperties.CookieProperties.class);
        when(cookieProperties.getCookieName()).thenReturn("apimlAuthenticationToken");
        when(authConfigurationProperties.getCookieProperties()).thenReturn(cookieProperties);
        X509Certificate certificate = X509Utils.getCertificate("zowe");
        X509AuthSource authSource = new X509AuthSource(certificate);
        zosmfScheme.createCommand(authentication, authSource).apply(null);

        verify(requestContext, times(1)).addZuulRequestHeader(anyString(), anyString());
    }


    @Test
    void givenNotValidToken_whenCreateCommand_thenThrowTokenNotValidException() {
        when(authSourceService.getAuthSourceFromRequest()).thenReturn(Optional.of(new JwtAuthSource("jwtToken3")));
        when(authSourceService.getLtpaToken(new JwtAuthSource("jwtToken3"))).thenThrow(new TokenNotValidException("Token is not valid"));
        when(authSourceService.parse(new JwtAuthSource("jwtToken3"))).thenReturn(parsedSourceZowe);

        AuthenticationCommand command = zosmfScheme.createCommand(authentication, new JwtAuthSource("jwtToken3"));
        Exception exception = assertThrows(TokenNotValidException.class, () -> command.apply(null), " Token is not valid");
        assertEquals("Token is not valid", exception.getMessage());

    }

    @Test
    void givenExpiredToken_whenCreateCommand_thenThrowJwtTokenException() {
        when(authSourceService.getAuthSourceFromRequest()).thenReturn(Optional.of(new JwtAuthSource("jwtToken3")));
        when(authSourceService.getLtpaToken(new JwtAuthSource("jwtToken3"))).thenThrow(new JwtException("Token is expired"));
        when(authSourceService.parse(new JwtAuthSource("jwtToken3"))).thenReturn(parsedSourceZowe);

        AuthenticationCommand command = zosmfScheme.createCommand(authentication, new JwtAuthSource("jwtToken3"));
        Exception exception = assertThrows(JwtException.class, () -> command.apply(null), "Token is expired");
        assertEquals("Token is expired", exception.getMessage());

    }

    @Test
    void givenZosmfScheme_whenGetScheme_thenReturnScheme() {
        assertEquals(AuthenticationScheme.ZOSMF, scheme.getScheme());
    }

    @Test
    void givenNoToken_whenCreateCommand_thenTestCommandExpiration() {
        AuthenticationCommand command = scheme.createCommand(null, null);

        assertNull(ReflectionTestUtils.getField(command, "expireAt"));
        assertFalse(command.isExpired());
    }

    @Test
    void givenTokenWithoutExpiration_whenCreateCommand_thenTestCommandExpiration() {
        when(authSourceService.parse(new JwtAuthSource("jwtToken"))).thenReturn(new JwtAuthSource.Parsed("user", null, null, Origin.ZOWE));

        AuthenticationCommand command = scheme.createCommand(null, new JwtAuthSource("jwtToken"));

        assertNull(ReflectionTestUtils.getField(command, "expireAt"));
        assertFalse(command.isExpired());
    }

    @Test
    void givenTokenWithExpiration_whenCreateCommand_thenTestCommandExpiration() {
        when(authSourceService.parse(new JwtAuthSource("jwtToken"))).thenReturn(new JwtAuthSource.Parsed("user", new Date(123), new Date(123), Origin.ZOWE));

        AuthenticationCommand command = scheme.createCommand(null, new JwtAuthSource("jwtToken"));

        assertNotNull(ReflectionTestUtils.getField(command, "expireAt"));
        assertEquals(123L, ReflectionTestUtils.getField(command, "expireAt"));
        assertTrue(command.isExpired());
    }

    private AuthSource.Parsed prepareParsedAuthSourceForTime(int amountOfSeconds) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.SECOND, amountOfSeconds);
        return new JwtAuthSource.Parsed("user", new Date(), c.getTime(), Origin.ZOWE);
    }

    @Test
    void givenTokenExpiredOneSecAgo_whenCreateCommand_thenTestCommandExpiration() {
        when(authSourceService.parse(new JwtAuthSource("jwtToken"))).thenReturn(prepareParsedAuthSourceForTime(-1));

        AuthenticationCommand command = scheme.createCommand(null, new JwtAuthSource("jwtToken"));

        assertTrue(command.isExpired());
    }

    @Test
    void givenTokenThatWillExpireInOneSec_whenCreateCommand_thenTestCommandExpiration() {
        when(authSourceService.parse(new JwtAuthSource("jwtToken"))).thenReturn(prepareParsedAuthSourceForTime(2));

        AuthenticationCommand command = scheme.createCommand(null, new JwtAuthSource("jwtToken"));

        assertFalse(command.isExpired());
    }

    @Test
    void givenZosmfToken_whenCreateCommand_thenTestJwtToken() {
        AuthConfigurationProperties.CookieProperties cookieProperties = mock(AuthConfigurationProperties.CookieProperties.class);
        when(cookieProperties.getCookieName()).thenReturn("apimlAuthenticationToken");
        when(authConfigurationProperties.getCookieProperties()).thenReturn(cookieProperties);

        ZosmfScheme scheme = new ZosmfScheme(authSourceService, authConfigurationProperties);
        when(authSourceService.getAuthSourceFromRequest()).thenReturn(Optional.of(new JwtAuthSource("jwtTokenZosmf")));
        when(authSourceService.parse(new JwtAuthSource("jwtTokenZosmf"))).thenReturn(parsedSourceZosmf);
        ReflectionTestUtils.setField(scheme, "authProvider", "zosmf");
        AuthenticationCommand command = scheme.createCommand(new Authentication(AuthenticationScheme.ZOSMF, null), new JwtAuthSource("jwtTokenZosmf"));

        command.apply(null);

        verify(authSourceService, times(1)).getAuthSourceFromRequest();
        verify(authSourceService, times(2)).parse(new JwtAuthSource("jwtTokenZosmf"));
        verify(authSourceService, never()).getLtpaToken(new JwtAuthSource("jwtTokenZosmf"));
    }

    @Test
    void givenNoJwtToken_whenCreateCommand_thenExpectValidJwtRequired() {
        AuthenticationCommand command = scheme.createCommand(new Authentication(AuthenticationScheme.ZOSMF, null), null);

        assertTrue(command.isRequiredValidSource());
    }

    private void prepareAuthenticationService(String ltpaToken) {
        when(authSourceService.getAuthSourceFromRequest()).thenReturn(Optional.of(new JwtAuthSource("jwtToken2")));
        when(authSourceService.getLtpaToken(new JwtAuthSource("jwtToken2"))).thenReturn(ltpaToken);
        when(authSourceService.parse(new JwtAuthSource("jwtToken2"))).thenReturn(parsedSourceZowe);
    }

    @Test
    void givenRequestWithSetCookie_whenApplyToRequest_thenAppendSetCookie() {
        HttpRequest httpRequest = new HttpGet("/test/request");
        httpRequest.setHeader(COOKIE_HEADER, "cookie1=1");
        prepareAuthenticationService("ltpa2");

        zosmfScheme.createCommand(authentication, new JwtAuthSource("jwtToken2")).applyToRequest(httpRequest);

        assertEquals("cookie1=1;LtpaToken2=ltpa2", httpRequest.getFirstHeader("cookie").getValue());
    }

    @Test
    void givenRequestWithNoCookie_whenApplyToRequest_thenAppendSetCookie() {
        HttpRequest httpRequest = new HttpGet("/test/request");
        prepareAuthenticationService("ltpa1");

        zosmfScheme.createCommand(authentication, new JwtAuthSource("jwtToken2")).applyToRequest(httpRequest);

        assertEquals("LtpaToken2=ltpa1", httpRequest.getFirstHeader("cookie").getValue());
    }

    @Test
    void givenClientCertificate_whenApplyToRequest_thenDontAddZuulHeader() {
        when(authSourceService.getAuthSourceFromRequest()).thenReturn(Optional.of(new X509AuthSource(mock(
            X509Certificate.class))));
        when(authSourceService.parse(any())).thenReturn(parsedSourceX509);

        zosmfScheme.createCommand(authentication, null).applyToRequest(null);

        verify(requestContext, never()).addZuulRequestHeader(anyString(), anyString());
    }

    @Test
    void givenRequest_whenApplyToRequest_thenSetAuthHeaderToNull() {
        HttpRequest httpRequest = new HttpGet("/test/request");
        prepareAuthenticationService("ltpa1");

        zosmfScheme.createCommand(authentication, new JwtAuthSource("jwtToken2")).applyToRequest(httpRequest);

        assertNull(httpRequest.getFirstHeader(HttpHeaders.AUTHORIZATION));
    }


    static Stream<Arguments> authSources() {
        return Stream.of(Arguments.of(new JwtAuthSource("jwtToken2")), Arguments.of(new X509AuthSource(mock(
            X509Certificate.class))));
    }

    @Nested
    class ValidAuthSourceTest {

        @ParameterizedTest(name = "whenApplyToRequest_thenTestJwtToken")
        @MethodSource("org.zowe.apiml.gateway.security.service.schema.ZosmfSchemeTest#authSources")
        void givenZosmfToken_whenApplyToRequest_thenTestJwtToken(AuthSource authSource) {
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

            when(authSourceService.getAuthSourceFromRequest()).thenReturn(Optional.of(authSource));
            when(authSourceService.parse(any(X509AuthSource.class))).thenReturn(parsedSourceX509);
            when(authSourceService.parse(any(JwtAuthSource.class))).thenReturn(parsedSourceZosmf);
            when(authConfigurationProperties.getCookieProperties().getCookieName()).thenReturn("apimlAuthenticationToken");
            when(authSourceService.getJWT(any(JwtAuthSource.class))).thenReturn("jwtToken2");
            when(authSourceService.getJWT(any(X509AuthSource.class))).thenReturn("jwtToken2");
            zosmfScheme.createCommand(authentication, authSource).applyToRequest(httpRequest);

            assertEquals("cookie1=1;jwtToken=jwtToken2", httpRequest.getFirstHeader("cookie").getValue());
        }

    }

}
