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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.gateway.security.service.PassTicketException;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSourceService;
import org.zowe.apiml.gateway.security.service.schema.source.DefaultAuthSourceService;
import org.zowe.apiml.gateway.security.service.schema.source.JwtAuthSource;
import org.zowe.apiml.gateway.security.service.schema.source.JwtAuthSourceService;
import org.zowe.apiml.gateway.security.service.schema.source.X509AuthSourceService;
import org.zowe.apiml.gateway.utils.CleanCurrentRequestContextTest;
import org.zowe.apiml.passticket.PassTicketService;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;

import javax.servlet.http.HttpServletRequest;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.zowe.apiml.passticket.PassTicketService.DefaultPassTicketImpl.UNKNOWN_USER;

class HttpBasicPassTicketSchemeTest extends CleanCurrentRequestContextTest {

    private static final String USERNAME = "USERNAME";
    private final AuthConfigurationProperties authConfigurationProperties = new AuthConfigurationProperties();
    private HttpBasicPassTicketScheme httpBasicPassTicketScheme;

    @BeforeEach
    void init() {
        JwtAuthSourceService jwtAuthSourceService = mock(JwtAuthSourceService.class);
        X509AuthSourceService x509MFAuthSourceService = mock(X509AuthSourceService.class);

        PassTicketService passTicketService = new PassTicketService();
        AuthSourceService authSourceService = new DefaultAuthSourceService(jwtAuthSourceService, x509MFAuthSourceService);
        httpBasicPassTicketScheme = new HttpBasicPassTicketScheme(passTicketService, authSourceService, authConfigurationProperties);
    }

    @AfterEach
    void tearEverythingDown() {
        RequestContext.testSetCurrentContext(null);
    }

    @Test
    void testCreateCommand() {
        PassTicketService passTicketService = new PassTicketService();
        AuthSourceService authSourceService = mock(AuthSourceService.class);
        httpBasicPassTicketScheme = new HttpBasicPassTicketScheme(passTicketService, authSourceService, authConfigurationProperties);

        Calendar calendar = Calendar.getInstance();
        Authentication authentication = new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "APPLID");
        AuthSource.Parsed parsedSource = new JwtAuthSource.Parsed(USERNAME, calendar.getTime(), calendar.getTime(), AuthSource.Origin.ZOWE);
        when(authSourceService.parse(new JwtAuthSource("token"))).thenReturn(parsedSource);
        AuthenticationCommand ac = httpBasicPassTicketScheme.createCommand(authentication, new JwtAuthSource("token"));
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

        // JWT token expired one minute ago (command expired also if JWT token expired)
        calendar.add(Calendar.MINUTE, -1);
        AuthSource.Parsed parsedSource2 = new JwtAuthSource.Parsed(USERNAME, calendar.getTime(), calendar.getTime(), AuthSource.Origin.ZOWE);
        when(authSourceService.parse(new JwtAuthSource("token"))).thenReturn(parsedSource2);
        ac = httpBasicPassTicketScheme.createCommand(authentication, new JwtAuthSource("token"));
        assertTrue(ac.isExpired());

        // JWT token will expire in one minute (command expired also if JWT token expired)
        calendar.add(Calendar.MINUTE, 2);
        AuthSource.Parsed parsedSource3 = new JwtAuthSource.Parsed(USERNAME, calendar.getTime(), calendar.getTime(), AuthSource.Origin.ZOWE);
        when(authSourceService.parse(new JwtAuthSource("token"))).thenReturn(parsedSource3);
        ac = httpBasicPassTicketScheme.createCommand(authentication, new JwtAuthSource("token"));
        assertFalse(ac.isExpired());

        calendar.add(Calendar.MINUTE, 100);
        AuthSource.Parsed parsedSource4 = new JwtAuthSource.Parsed(USERNAME, calendar.getTime(), calendar.getTime(), AuthSource.Origin.ZOWE);
        when(authSourceService.parse(new JwtAuthSource("token"))).thenReturn(parsedSource4);
        ac = httpBasicPassTicketScheme.createCommand(authentication, new JwtAuthSource("token"));

        calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, authConfigurationProperties.getPassTicket().getTimeout());
        // checking setup of expired time, JWT expired in future (more than hour), check if set date is similar to passticket timeout (5s)
        assertEquals(0.0, Math.abs(calendar.getTime().getTime() - (long) ReflectionTestUtils.getField(ac, "expireAt")), 10.0);
    }

    @Test
    void returnsCorrectScheme() {
        assertEquals(AuthenticationScheme.HTTP_BASIC_PASSTICKET, httpBasicPassTicketScheme.getScheme());
    }

    @Test
    void getExceptionWhenUserIdNotValid() {
        String applId = "APPLID";
        PassTicketService passTicketService = new PassTicketService();
        AuthSourceService authSourceService = mock(AuthSourceService.class);
        httpBasicPassTicketScheme = new HttpBasicPassTicketScheme(passTicketService, authSourceService, authConfigurationProperties);

        Calendar calendar = Calendar.getInstance();
        Authentication authentication = new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, applId);
        AuthSource.Parsed parsedSource = new JwtAuthSource.Parsed(UNKNOWN_USER, calendar.getTime(), calendar.getTime(), AuthSource.Origin.ZOWE);
        when(authSourceService.parse(new JwtAuthSource("token"))).thenReturn(parsedSource);
        AuthSource authSource = new JwtAuthSource("token");
        Exception exception = assertThrows(PassTicketException.class,
            () -> httpBasicPassTicketScheme.createCommand(authentication, authSource),
            "Expected exception is not AuthenticationException");
        assertEquals((String.format("Could not generate PassTicket for user ID %s and APPLID %s", UNKNOWN_USER, applId)), exception.getMessage());
    }

    @Test
    void testIsRequiredValidJwt() {
        PassTicketService passTicketService = new PassTicketService();
        AuthSourceService authSourceService = mock(AuthSourceService.class);
        httpBasicPassTicketScheme = new HttpBasicPassTicketScheme(passTicketService, authSourceService, authConfigurationProperties);

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, 1);
        Authentication authentication = new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid");
        AuthSource.Parsed parsedSource = new JwtAuthSource.Parsed("username", calendar.getTime(), calendar.getTime(), AuthSource.Origin.ZOWE);
        when(authSourceService.parse(new JwtAuthSource("token"))).thenReturn(parsedSource);
        AuthenticationCommand ac = httpBasicPassTicketScheme.createCommand(authentication, new JwtAuthSource("token"));
        assertTrue(ac.isRequiredValidSource());
    }

    @Test
    void whenCallWithoutJwt_thenDoNothing() {
        Authentication authentication = new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid");
        AuthenticationCommand ac = httpBasicPassTicketScheme.createCommand(authentication, null);
        assertSame(AuthenticationCommand.EMPTY, ac);
    }

    private HttpBasicPassTicketScheme.PassTicketCommand getPassTicketCommand() {
        PassTicketService passTicketService = new PassTicketService();
        AuthSourceService authSourceService = mock(AuthSourceService.class);
        httpBasicPassTicketScheme = new HttpBasicPassTicketScheme(passTicketService, authSourceService, authConfigurationProperties);

        Calendar c = Calendar.getInstance();
        c.add(Calendar.YEAR, 1);

        Authentication authentication = new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "APPLID");
        AuthSource.Parsed parsedSource = new JwtAuthSource.Parsed(USERNAME, new Date(), c.getTime(), AuthSource.Origin.ZOWE);
        when(authSourceService.parse(new JwtAuthSource("token"))).thenReturn(parsedSource);
        AuthenticationCommand out = httpBasicPassTicketScheme.createCommand(authentication, new JwtAuthSource("token"));
        assertTrue(out instanceof HttpBasicPassTicketScheme.PassTicketCommand);
        return (HttpBasicPassTicketScheme.PassTicketCommand) out;
    }

    @Test
    void givenJwtInCookie_whenApply_thenJwtIsRemoved() {
        AuthenticationCommand command = getPassTicketCommand();
        RequestContext requestContext = new RequestContext();
        requestContext.addZuulRequestHeader("cookie",
            authConfigurationProperties.getCookieProperties().getCookieName() + "=jwt;" +
                "abc=def"
        );
        RequestContext.testSetCurrentContext(requestContext);

        command.apply(null);

        String cookies = requestContext.getZuulRequestHeaders().get("cookie");
        assertEquals("abc=def", cookies);
    }

}
