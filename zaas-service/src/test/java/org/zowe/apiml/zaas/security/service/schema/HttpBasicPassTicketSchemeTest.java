/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.service.schema;

import com.netflix.zuul.context.RequestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.zaas.security.service.schema.source.*;
import org.zowe.apiml.zaas.utils.CleanCurrentRequestContextTest;
import org.zowe.apiml.passticket.PassTicketService;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
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
            doReturn(Optional.empty()).when(authSourceService).getAuthSourceFromRequest(any());

            httpBasicPassTicketScheme.getAuthSource();
            verify(authSourceService, times(1)).getAuthSourceFromRequest(any());
        }

        @Test
        void returnsCorrectScheme() {
            assertEquals(AuthenticationScheme.HTTP_BASIC_PASSTICKET, httpBasicPassTicketScheme.getScheme());
        }

        @Test
        void testIsRequiredValidJwt() {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, 1);
            AuthSource.Parsed parsedSource = new ParsedTokenAuthSource("username", calendar.getTime(), calendar.getTime(), AuthSource.Origin.ZOWE);
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
            AuthSource.Parsed parsedSource = new ParsedTokenAuthSource(null, calendar.getTime(), calendar.getTime(), AuthSource.Origin.ZOWE);
            doReturn(parsedSource).when(authSourceService).parse(any(AuthSource.class));
            assertThrows(AuthSchemeException.class, () -> httpBasicPassTicketScheme.createCommand(authentication, jwtAuthSource));
        }
    }

    @Test
    void testCreateCommand() {
        Calendar calendar = Calendar.getInstance();
        AuthSource.Parsed parsedSource = new ParsedTokenAuthSource(USERNAME, calendar.getTime(), calendar.getTime(), AuthSource.Origin.ZOWE);
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
        void commandIsAlwaysExpired() {
            Calendar calendar = Calendar.getInstance();
            AuthSource.Parsed parsedSource = new ParsedTokenAuthSource(USERNAME, calendar.getTime(), calendar.getTime(), AuthSource.Origin.ZOWE);
            when(authSourceService.parse(jwtAuthSource)).thenReturn(parsedSource);
            ac = httpBasicPassTicketScheme.createCommand(authentication, jwtAuthSource);
            // need to wait here a little so that we don't test expiration time in the same millisecond.
            await().pollDelay(100, TimeUnit.MILLISECONDS).until(() -> true);
            assertTrue(ac.isExpired());
        }
    }

    @Nested
    class GivenZoweJwtAuthSource {

        @Test
        void getExceptionWhenUserIdNotValid() {
            Calendar calendar = Calendar.getInstance();
            AuthSource.Parsed parsedSource = new ParsedTokenAuthSource(UNKNOWN_USER, calendar.getTime(), calendar.getTime(), AuthSource.Origin.ZOWE);
            when(authSourceService.parse(jwtAuthSource)).thenReturn(parsedSource);
            assertThrows(AuthSchemeException.class, () -> httpBasicPassTicketScheme.createCommand(authentication, jwtAuthSource));
        }

        @Test
        void givenJwtInCookie_whenApply_thenJwtIsRemoved() {
            AuthenticationCommand command = getPassTicketCommand();
            RequestContext requestContext = new RequestContext();
            HttpServletRequest request = new MockHttpServletRequest();
            ((MockHttpServletRequest) request).addHeader("cookie",
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
            String customHeader = requestContext.getZuulRequestHeaders().get("header");
            String customUserHeader = requestContext.getZuulRequestHeaders().get("userid");
            assertNull(customHeader);
            assertNull(customUserHeader);
        }

        @Test
        void givenCustomAuthHeader_whenApply_thenHeaderIsAdded() {
            ReflectionTestUtils.setField(httpBasicPassTicketScheme, "customPassTicketHeader", "header");
            ReflectionTestUtils.setField(httpBasicPassTicketScheme, "customUserHeader", "userid");
            AuthenticationCommand command = getPassTicketCommand();
            RequestContext requestContext = new RequestContext();
            HttpServletRequest request = new MockHttpServletRequest();

            requestContext.setRequest(request);

            RequestContext.testSetCurrentContext(requestContext);

            command.apply(null);

            String customAuthHeader = requestContext.getZuulRequestHeaders().get("header");
            String customUserHeader = requestContext.getZuulRequestHeaders().get("userid");
            assertNotNull(customAuthHeader);
            assertNotNull(customUserHeader);
        }

        private HttpBasicPassTicketScheme.PassTicketCommand getPassTicketCommand() {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.YEAR, 1);

            AuthSource.Parsed parsedSource = new ParsedTokenAuthSource(USERNAME, new Date(), c.getTime(), AuthSource.Origin.ZOWE);
            when(authSourceService.parse(jwtAuthSource)).thenReturn(parsedSource);
            AuthenticationCommand out = httpBasicPassTicketScheme.createCommand(authentication, jwtAuthSource);
            assertTrue(out instanceof HttpBasicPassTicketScheme.PassTicketCommand);
            return (HttpBasicPassTicketScheme.PassTicketCommand) out;
        }
    }
}
