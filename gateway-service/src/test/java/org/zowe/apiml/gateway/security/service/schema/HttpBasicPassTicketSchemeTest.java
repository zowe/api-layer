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
import java.util.Optional;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.HttpGet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSchemeException;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSourceService;
import org.zowe.apiml.gateway.security.service.schema.source.JwtAuthSource;
import org.zowe.apiml.gateway.security.service.schema.source.X509AuthSource;
import org.zowe.apiml.gateway.utils.CleanCurrentRequestContextTest;
import org.zowe.apiml.passticket.IRRPassTicketGenerationException;
import org.zowe.apiml.passticket.PassTicketService;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;

import javax.servlet.http.HttpServletRequest;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import org.zowe.apiml.security.common.token.TokenExpireException;
import org.zowe.apiml.security.common.token.TokenNotValidException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.zowe.apiml.passticket.PassTicketService.DefaultPassTicketImpl.UNKNOWN_USER;

class HttpBasicPassTicketSchemeTest extends CleanCurrentRequestContextTest {

    private static final String USERNAME = "USERNAME";
    private final AuthConfigurationProperties authConfigurationProperties = new AuthConfigurationProperties();
    private final Authentication authentication = new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "APPLID");
    private final AuthSource jwtAuthSource = new JwtAuthSource("jwtToken1");
    private HttpBasicPassTicketScheme httpBasicPassTicketScheme;
    private PassTicketService passTicketService;
    private AuthSourceService authSourceService;

    @BeforeEach
    void init() {
        passTicketService = spy(new PassTicketService());
        authSourceService = mock(AuthSourceService.class);
        httpBasicPassTicketScheme = new HttpBasicPassTicketScheme(passTicketService, authSourceService, authConfigurationProperties);
    }

    @AfterEach
    void tearEverythingDown() {
        RequestContext.testSetCurrentContext(null);
    }

    @Nested
    class AuthSourceIndependentTests {
        @Test
        void testGetAuthSource() {
            doReturn(Optional.empty()).when(authSourceService).getAuthSourceFromRequest();

            httpBasicPassTicketScheme.getAuthSource();
            verify(authSourceService, times(1)).getAuthSourceFromRequest();
        }

        @Test
        void returnsCorrectScheme() {
            assertEquals(AuthenticationScheme.HTTP_BASIC_PASSTICKET, httpBasicPassTicketScheme.getScheme());
        }

        @Test
        void testIsRequiredValidJwt() {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, 1);
            AuthSource.Parsed parsedSource = new JwtAuthSource.Parsed("username", calendar.getTime(), calendar.getTime(), AuthSource.Origin.ZOWE);
            when(authSourceService.parse(jwtAuthSource)).thenReturn(parsedSource);
            AuthenticationCommand ac = httpBasicPassTicketScheme.createCommand(authentication, jwtAuthSource);
            assertTrue(ac.isRequiredValidSource());
        }

        @Test
        void givenNoAuthSource_thenThrows() {
            assertThrows(AuthSchemeException.class, () -> httpBasicPassTicketScheme.createCommand(authentication, null));
        }

        @Test
        void givenAuthSourceWithoutContent_thenThrows() {
            AuthSource authSource = new X509AuthSource(null);
            assertThrows(AuthSchemeException.class, () -> httpBasicPassTicketScheme.createCommand(authentication, authSource));
        }

        @Test
        void givenNullParsingResult_thenThrows() {
            doReturn(null).when(authSourceService).parse(any(AuthSource.class));
            assertThrows(IllegalStateException.class, () -> httpBasicPassTicketScheme.createCommand(authentication, jwtAuthSource));
        }

        @Test
        void givenNoUseridInParsingResult_thenThrows() {
            Calendar calendar = Calendar.getInstance();
            AuthSource.Parsed parsedSource = new JwtAuthSource.Parsed(null, calendar.getTime(), calendar.getTime(), AuthSource.Origin.ZOWE);
            doReturn(parsedSource).when(authSourceService).parse(any(AuthSource.class));
            assertThrows(AuthSchemeException.class, () -> httpBasicPassTicketScheme.createCommand(authentication, jwtAuthSource));
        }
    }

    @Test
    void testCreateCommand() {
        Calendar calendar = Calendar.getInstance();
        AuthSource.Parsed parsedSource = new JwtAuthSource.Parsed(USERNAME, calendar.getTime(), calendar.getTime(), AuthSource.Origin.ZOWE);
        when(authSourceService.parse(jwtAuthSource)).thenReturn(parsedSource);
        AuthenticationCommand ac = httpBasicPassTicketScheme.createCommand(authentication, jwtAuthSource);
        assertNotNull(ac);

        RequestContext requestContext = new RequestContext();
        HttpServletRequest request = new MockHttpServletRequest();
        requestContext.setRequest(request);
        RequestContext.testSetCurrentContext(requestContext);
        ac.apply(null);

        String authorizationValue = new String(
            Base64.getDecoder().decode(
                requestContext.getZuulRequestHeaders().get("authorization").split(" ")[1]
            )
        );
        assertTrue(authorizationValue.startsWith("USERNAME:ZOWE_DUMMY_PASS_TICKET_APPLID_USERNAME_"));
    }

    @Nested
    class ExpirationTest {
        AuthenticationCommand ac;

        @Test
        void whenJwtExpired_thenCommandExpired() {
            // JWT token expired one minute ago (command expired also if JWT token expired)
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MINUTE, -1);
            AuthSource.Parsed parsedSource = new JwtAuthSource.Parsed(USERNAME, calendar.getTime(), calendar.getTime(), AuthSource.Origin.ZOWE);
            when(authSourceService.parse(jwtAuthSource)).thenReturn(parsedSource);
            ac = httpBasicPassTicketScheme.createCommand(authentication, jwtAuthSource);
            assertTrue(ac.isExpired());
        }

        @Test
        void whenJwtExpireSoon_thenCommandInNotExpiredYet() {
            // JWT token will expire in one minute (command expired also if JWT token expired)
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MINUTE, 2);
            AuthSource.Parsed parsedSource = new JwtAuthSource.Parsed(USERNAME, calendar.getTime(), calendar.getTime(), AuthSource.Origin.ZOWE);
            when(authSourceService.parse(jwtAuthSource)).thenReturn(parsedSource);
            ac = httpBasicPassTicketScheme.createCommand(authentication, jwtAuthSource);
            assertFalse(ac.isExpired());
        }

        @Test
        void testPassticketTimeout() {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MINUTE, 100);
            AuthSource.Parsed parsedSource4 = new JwtAuthSource.Parsed(USERNAME, calendar.getTime(), calendar.getTime(), AuthSource.Origin.ZOWE);
            when(authSourceService.parse(jwtAuthSource)).thenReturn(parsedSource4);
            ac = httpBasicPassTicketScheme.createCommand(authentication, jwtAuthSource);

            calendar = Calendar.getInstance();
            calendar.add(Calendar.SECOND, authConfigurationProperties.getPassTicket().getTimeout());
            // checking setup of expired time, JWT expired in future (more than hour), check if set date is similar to passticket timeout (5s)
            assertEquals(0.0, Math.abs(calendar.getTime().getTime() - (long) ReflectionTestUtils.getField(ac, "expireAt")), 10.0);
        }
    }

    @Nested
    class GivenZoweJwtAuthSource {
        @Test
        void givenRequest_whenApplyToRequest_thenSetsAuthorizationBasic() throws IRRPassTicketGenerationException {
            Calendar calendar = Calendar.getInstance();
            AuthSource.Parsed parsedSource = new JwtAuthSource.Parsed(USERNAME, calendar.getTime(), calendar.getTime(), AuthSource.Origin.ZOWE);
            when(authSourceService.parse(jwtAuthSource)).thenReturn(parsedSource);
            HttpRequest httpRequest = new HttpGet("/test/request");

            RequestContext requestContext = new RequestContext();
            RequestContext.testSetCurrentContext(requestContext);

            doReturn("HI").when(passTicketService).generate(ArgumentMatchers.any(), ArgumentMatchers.any());

            AuthenticationCommand ac = httpBasicPassTicketScheme.createCommand(authentication, jwtAuthSource);
            ac.applyToRequest(httpRequest);
            assertThat(httpRequest.getHeaders(HttpHeaders.AUTHORIZATION).length, is(not(0)));
            assertThat(httpRequest.getHeaders(HttpHeaders.AUTHORIZATION), hasItemInArray(hasToString(
                "Authorization: Basic VVNFUk5BTUU6SEk=" // USERNAME:HI
            )));
        }

        @Test
        void getExceptionWhenUserIdNotValid() {
            Calendar calendar = Calendar.getInstance();
            AuthSource.Parsed parsedSource = new JwtAuthSource.Parsed(UNKNOWN_USER, calendar.getTime(), calendar.getTime(), AuthSource.Origin.ZOWE);
            when(authSourceService.parse(jwtAuthSource)).thenReturn(parsedSource);
            assertThrows(AuthSchemeException.class, () -> httpBasicPassTicketScheme.createCommand(authentication, jwtAuthSource));
        }

        @Test
        void givenJwtInCookie_whenApply_thenJwtIsRemoved() {
            AuthenticationCommand command = getPassTicketCommand();
            RequestContext requestContext = new RequestContext();
            HttpServletRequest request = new MockHttpServletRequest();
            ((MockHttpServletRequest)request).addHeader("cookie",
                authConfigurationProperties.getCookieProperties().getCookieName() + "=jwt;" +
                    "abc=def");

            requestContext.setRequest(request);
            requestContext.addZuulRequestHeader("cookie",
                authConfigurationProperties.getCookieProperties().getCookieName() + "=jwt;" +
                    "abc=def"
            );
            RequestContext.testSetCurrentContext(requestContext);

            command.apply(null);

            String cookies = requestContext.getZuulRequestHeaders().get("cookie");
            assertEquals("abc=def", cookies);
        }

        @Test
        void givenInvalidZoweJwtAuthSource_thenSetErrorHeader() {
            when(authSourceService.parse(jwtAuthSource)).thenThrow(new TokenNotValidException("Token is not valid"));

            assertThrows(AuthSchemeException.class, () -> httpBasicPassTicketScheme.createCommand(authentication, jwtAuthSource));
        }

        @Test
        void givenExpiredZoweJwtAuthSource_thenThrowJwtTokenException() {
            when(authSourceService.parse(jwtAuthSource)).thenThrow(new TokenExpireException("Token is expired"));

            assertThrows(AuthSchemeException.class, () -> httpBasicPassTicketScheme.createCommand(authentication, jwtAuthSource));
        }

        @Test
        void givenNoCookie_whenApplyToRequest_thenNoCookies() {
            AuthenticationCommand command = getPassTicketCommand();
            HttpRequest httpRequest = new HttpGet();

            command.applyToRequest(httpRequest);

            assertEquals(0, httpRequest.getHeaders("cookie").length);
        }

        @Test
        void givenJwtInCookie_whenApplyToRequest_thenJwtIsRemoved() {
            AuthenticationCommand command = getPassTicketCommand();
            HttpRequest httpRequest = new HttpGet();
            httpRequest.setHeader("cookie",
                authConfigurationProperties.getCookieProperties().getCookieName() + "=jwt;" +
                    "abc=def"
            );

            command.applyToRequest(httpRequest);

            Header[] headers = httpRequest.getHeaders("cookie");
            assertNotNull(headers);
            assertEquals(1, headers.length);
            assertEquals("abc=def", headers[0].getValue());
        }
    }

    private HttpBasicPassTicketScheme.PassTicketCommand getPassTicketCommand() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.YEAR, 1);

        AuthSource.Parsed parsedSource = new JwtAuthSource.Parsed(USERNAME, new Date(), c.getTime(), AuthSource.Origin.ZOWE);
        when(authSourceService.parse(jwtAuthSource)).thenReturn(parsedSource);
        AuthenticationCommand out = httpBasicPassTicketScheme.createCommand(authentication, jwtAuthSource);
        assertTrue(out instanceof HttpBasicPassTicketScheme.PassTicketCommand);
        return (HttpBasicPassTicketScheme.PassTicketCommand) out;
    }
}
