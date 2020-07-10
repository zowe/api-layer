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
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.HttpGet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.gateway.security.service.PassTicketException;
import org.zowe.apiml.gateway.utils.CleanCurrentRequestContextTest;
import org.zowe.apiml.passticket.IRRPassTicketGenerationException;
import org.zowe.apiml.passticket.PassTicketService;
import org.zowe.apiml.security.common.auth.Authentication;
import org.zowe.apiml.security.common.auth.AuthenticationScheme;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.token.QueryResponse;

import javax.servlet.http.HttpServletRequest;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.zowe.apiml.passticket.PassTicketService.DefaultPassTicketImpl.UNKNOWN_USER;

class HttpBasicPassTicketSchemeTest extends CleanCurrentRequestContextTest {

    private static final String USERNAME = "USERNAME";
    private final AuthConfigurationProperties authConfigurationProperties = new AuthConfigurationProperties();
    private HttpBasicPassTicketScheme httpBasicPassTicketScheme;

    @BeforeEach
    void init() {
        PassTicketService passTicketService = new PassTicketService();
        httpBasicPassTicketScheme = new HttpBasicPassTicketScheme(passTicketService, authConfigurationProperties);
    }

    @AfterEach
    void tearEverythingDown() {
        RequestContext.testSetCurrentContext(null);
    }

    @Test
    void testCreateCommand() {
        Calendar calendar = Calendar.getInstance();
        Authentication authentication = new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "APPLID");
        QueryResponse queryResponse = new QueryResponse("domain", USERNAME, calendar.getTime(), calendar.getTime(), QueryResponse.Source.ZOWE);

        AuthenticationCommand ac = httpBasicPassTicketScheme.createCommand(authentication, () -> queryResponse);
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
        QueryResponse queryResponse2 = new QueryResponse("domain", USERNAME, calendar.getTime(), calendar.getTime(), QueryResponse.Source.ZOWE);
        ac = httpBasicPassTicketScheme.createCommand(authentication, () -> queryResponse2);
        assertTrue(ac.isExpired());

        // JWT token will expire in one minute (command expired also if JWT token expired)
        calendar.add(Calendar.MINUTE, 2);
        QueryResponse queryResponse3 = new QueryResponse("domain", USERNAME, calendar.getTime(), calendar.getTime(), QueryResponse.Source.ZOWE);
        ac = httpBasicPassTicketScheme.createCommand(authentication, () -> queryResponse3);
        assertFalse(ac.isExpired());

        calendar.add(Calendar.MINUTE, 100);
        QueryResponse queryResponse4 = new QueryResponse("domain", USERNAME, calendar.getTime(), calendar.getTime(), QueryResponse.Source.ZOWE);
        ac = httpBasicPassTicketScheme.createCommand(authentication, () -> queryResponse4);

        calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, authConfigurationProperties.getPassTicket().getTimeout());
        // checking setup of expired time, JWT expired in future (more than hour), check if set date is similar to passticket timeout (5s)
        assertEquals(0.0, Math.abs(calendar.getTime().getTime() - (long) ReflectionTestUtils.getField(ac, "expireAt")), 10.0);
    }

    @Test
    void givenRequest_whenApplyToRequest_thenSetsAuthorizationBasic() throws IRRPassTicketGenerationException {
        PassTicketService passTicketService = mock(PassTicketService.class);
        httpBasicPassTicketScheme = new HttpBasicPassTicketScheme(passTicketService, authConfigurationProperties);

        Calendar calendar = Calendar.getInstance();
        Authentication authentication = new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "APPLID");
        QueryResponse queryResponse = new QueryResponse("domain", USERNAME, calendar.getTime(), calendar.getTime(), QueryResponse.Source.ZOWE);
        HttpRequest httpRequest = new HttpGet("/test/request");

        RequestContext requestContext = new RequestContext();
        RequestContext.testSetCurrentContext(requestContext);

        doReturn("HI").when(passTicketService).generate(ArgumentMatchers.any(), ArgumentMatchers.any());

        AuthenticationCommand ac = httpBasicPassTicketScheme.createCommand(authentication, () -> queryResponse);
        ac.applyToRequest(httpRequest);
        assertThat(httpRequest.getHeaders(HttpHeaders.AUTHORIZATION).length, is(not(0)));
        assertThat(httpRequest.getHeaders(HttpHeaders.AUTHORIZATION), hasItemInArray(hasToString(
            "Authorization: Basic VVNFUk5BTUU6SEk=" // USERNAME:HI
        )));
    }

    @Test
    void returnsCorrectScheme() {
        assertEquals(AuthenticationScheme.HTTP_BASIC_PASSTICKET, httpBasicPassTicketScheme.getScheme());
    }

    @Test
    void getExceptionWhenUserIdNotValid() {
        String applId = "APPLID";

        Calendar calendar = Calendar.getInstance();
        Authentication authentication = new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, applId);
        QueryResponse queryResponse = new QueryResponse("domain", UNKNOWN_USER, calendar.getTime(), calendar.getTime(), QueryResponse.Source.ZOWE);
        Exception exception = assertThrows(PassTicketException.class,
            () -> httpBasicPassTicketScheme.createCommand(authentication, () -> queryResponse),
            "Expected exception is not AuthenticationException");
        assertEquals((String.format("Could not generate PassTicket for user ID %s and APPLID %s", UNKNOWN_USER, applId)), exception.getMessage());
    }

    @Test
    void testIsRequiredValidJwt() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, 1);
        Authentication authentication = new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid");
        QueryResponse queryResponse = new QueryResponse("domain", "username", calendar.getTime(), calendar.getTime(), QueryResponse.Source.ZOWE);
        AuthenticationCommand ac = httpBasicPassTicketScheme.createCommand(authentication, () -> queryResponse);
        assertTrue(ac.isRequiredValidJwt());
    }

    @Test
    void whenCallWithoutJwt_thenDoNothing() {
        Authentication authentication = new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid");
        AuthenticationCommand ac = httpBasicPassTicketScheme.createCommand(authentication, () -> null);
        assertSame(AuthenticationCommand.EMPTY, ac);
    }

    private HttpBasicPassTicketScheme.PassTicketCommand getPassTicketCommand() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.YEAR, 1);

        Authentication authentication = new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "APPLID");
        QueryResponse queryResponse = new QueryResponse("domain", USERNAME, new Date(), c.getTime(), QueryResponse.Source.ZOWE);
        AuthenticationCommand out = httpBasicPassTicketScheme.createCommand(authentication, () -> queryResponse);
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
