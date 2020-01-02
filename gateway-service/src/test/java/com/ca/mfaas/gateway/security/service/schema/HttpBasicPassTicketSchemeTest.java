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
import com.ca.apiml.security.common.service.PassTicketService;
import com.ca.apiml.security.common.token.QueryResponse;
import com.netflix.zuul.context.RequestContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Calendar;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HttpBasicPassTicketSchemeTest {

    private final int PASSTICKET_DURATION = 300;

    @Mock
    private PassTicketService passTicketService;

    @InjectMocks
    private HttpBasicPassTicketScheme httpBasicPassTicketScheme;

    @Before
    public void init() {
        ReflectionTestUtils.setField(httpBasicPassTicketScheme, "timeout", PASSTICKET_DURATION);
    }

    @Test
    public void testCreateCommand() {
        Calendar calendar = Calendar.getInstance();
        Authentication authentication = new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid");
        QueryResponse queryResponse = new QueryResponse("domain", "username", calendar.getTime(), calendar.getTime());

        when(passTicketService.generate("username", "applid")).thenReturn("123456");

        AuthenticationCommand ac = httpBasicPassTicketScheme.createCommand(authentication, queryResponse);
        assertNotNull(ac);

        RequestContext requestContext = new RequestContext();
        HttpServletRequest request = new MockHttpServletRequest();
        requestContext.setRequest(request);
        RequestContext.testSetCurrentContext(requestContext);
        ac.apply(null);

        assertEquals("Basic dXNlcm5hbWU6MTIzNDU2", requestContext.getZuulRequestHeaders().get("authorization"));

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
        calendar.add(Calendar.SECOND, PASSTICKET_DURATION);
        // checking setup of expired time, JWT expired in future (more than hour), check if set date is similar to passticket timeout (5s)
        assertTrue(Math.abs(calendar.getTime().getTime() - (long) ReflectionTestUtils.getField(ac, "expireAt")) < 5);
    }

}
