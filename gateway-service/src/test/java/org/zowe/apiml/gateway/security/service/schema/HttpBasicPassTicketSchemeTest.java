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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.gateway.security.service.AuthenticationException;
import org.zowe.apiml.security.common.auth.Authentication;
import org.zowe.apiml.security.common.auth.AuthenticationScheme;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.token.QueryResponse;
import org.zowe.apiml.gateway.utils.CleanCurrentRequestContextTest;
import org.zowe.apiml.passticket.PassTicketService;
import com.netflix.zuul.context.RequestContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Calendar;

import static org.junit.jupiter.api.Assertions.*;
import static org.zowe.apiml.passticket.PassTicketService.DefaultPassTicketImpl.UNKNOWN_USER;
import static org.junit.Assert.assertEquals;

public class HttpBasicPassTicketSchemeTest extends CleanCurrentRequestContextTest {
    private final AuthConfigurationProperties authConfigurationProperties = new AuthConfigurationProperties();
    private HttpBasicPassTicketScheme httpBasicPassTicketScheme;

    @BeforeEach
    public void init() {
        PassTicketService passTicketService = new PassTicketService();
        httpBasicPassTicketScheme = new HttpBasicPassTicketScheme(passTicketService, authConfigurationProperties);
    }

    @Test
    public void testCreateCommand() throws Exception {
        Calendar calendar = Calendar.getInstance();
        Authentication authentication = new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid");
        QueryResponse queryResponse = new QueryResponse("domain", "username", calendar.getTime(), calendar.getTime());

        AuthenticationCommand ac = httpBasicPassTicketScheme.createCommand(authentication, queryResponse);
        assertNotNull(ac);

        RequestContext requestContext = new RequestContext();
        HttpServletRequest request = new MockHttpServletRequest();
        requestContext.setRequest(request);
        RequestContext.testSetCurrentContext(requestContext);
        ac.apply(null);

        assertEquals("Basic dXNlcm5hbWU6Wm93ZUR1bW15UGFzc1RpY2tldF9hcHBsaWRfdXNlcm5hbWVfMA==",
            requestContext.getZuulRequestHeaders().get("authorization"));

        // JWT token expired one minute ago (command expired also if JWT token expired)
        calendar.add(Calendar.MINUTE, -1);
        queryResponse = new QueryResponse("domain", "username", calendar.getTime(), calendar.getTime());
        ac = httpBasicPassTicketScheme.createCommand(authentication, queryResponse);
        assertTrue(ac.isExpired());

        // JWT token will expire in one minute (command expired also if JWT token expired)
        calendar.add(Calendar.MINUTE, 2);
        queryResponse = new QueryResponse("domain", "username", calendar.getTime(), calendar.getTime());
        ac = httpBasicPassTicketScheme.createCommand(authentication, queryResponse);
        assertFalse(ac.isExpired());

        calendar.add(Calendar.MINUTE, 100);
        queryResponse = new QueryResponse("domain", "username", calendar.getTime(), calendar.getTime());
        ac = httpBasicPassTicketScheme.createCommand(authentication, queryResponse);

        calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, authConfigurationProperties.getPassTicket().getTimeout());
        // checking setup of expired time, JWT expired in future (more than hour), check if set date is similar to passticket timeout (5s)
        assertEquals(0.0, Math.abs(calendar.getTime().getTime() - (long) ReflectionTestUtils.getField(ac, "expireAt")), 10.0);
    }

    @Test
    public void returnsCorrectScheme() {
        assertEquals(AuthenticationScheme.HTTP_BASIC_PASSTICKET, httpBasicPassTicketScheme.getScheme());
    }

    @Test
    public void getExceptionWhenUserIdNotValid() throws AuthenticationException {
        String applId = "applId";

        Calendar calendar = Calendar.getInstance();
        Authentication authentication = new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, applId);
        QueryResponse queryResponse = new QueryResponse("domain", UNKNOWN_USER, calendar.getTime(), calendar.getTime());
        Exception exception = assertThrows(AuthenticationException.class, () -> {
            httpBasicPassTicketScheme.createCommand(authentication, queryResponse);
        });
        assertEquals((String.format("Could not generate PassTicket for user ID %s and APPLID %s", UNKNOWN_USER, applId)), exception.getMessage());
    }
}
