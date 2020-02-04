/*
* This program and the accompanying materials are made available under the terms of the
* Eclipse Public License v2.0 which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Copyright Contributors to the Zowe Project.
*/
package org.zowe.apiml.gateway.filters.pre;

import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.gateway.security.service.ServiceAuthenticationServiceImpl;
import org.zowe.apiml.gateway.security.service.schema.AuthenticationCommand;
import org.zowe.apiml.gateway.utils.CleanCurrentRequestContextTest;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.netflix.zuul.monitoring.CounterFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.cloud.netflix.zuul.util.ZuulRuntimeException;
import org.springframework.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVICE_ID_KEY;

@RunWith(MockitoJUnitRunner.class)
public class ServiceAuthenticationFilterTest extends CleanCurrentRequestContextTest {

    @Mock
    private ServiceAuthenticationServiceImpl serviceAuthenticationService;

    @InjectMocks
    private ServiceAuthenticationFilter serviceAuthenticationFilter;

    @Mock
    private AuthenticationCommand command;

    @Mock
    private AuthenticationService authenticationService;

    @Before
    public void init() throws Exception {
        when(serviceAuthenticationService.getAuthenticationCommand(anyString(), any())).thenReturn(command);
    }

    @Test
    public void testConfig() {
        assertEquals("pre", serviceAuthenticationFilter.filterType());
        assertEquals(9, serviceAuthenticationFilter.filterOrder());
        assertTrue(serviceAuthenticationFilter.shouldFilter());
    }

    @Test
    public void testRun() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);

        RequestContext requestContext = mock(RequestContext.class);
        when(requestContext.getRequest()).thenReturn(request);
        when(requestContext.get(SERVICE_ID_KEY)).thenReturn("service");
        RequestContext.testSetCurrentContext(requestContext);

        when(authenticationService.getJwtTokenFromRequest(any())).thenReturn(Optional.of("token"));

        serviceAuthenticationFilter.run();
        verify(serviceAuthenticationService, times(1)).getAuthenticationCommand("service", "token");
        verify(command, times(1)).apply(null);

        when(authenticationService.getJwtTokenFromRequest(any())).thenReturn(Optional.empty());
        serviceAuthenticationFilter.run();
        verify(serviceAuthenticationService, times(1)).getAuthenticationCommand(anyString(), any());

        reset(requestContext);
        reset(authenticationService);
        CounterFactory.initialize(new CounterFactory() {
            @Override
            public void increment(String name) {
            }
        });
        when(requestContext.get(SERVICE_ID_KEY)).thenReturn("error");
        when(authenticationService.getJwtTokenFromRequest(any())).thenReturn(Optional.of("token"));
        when(serviceAuthenticationService.getAuthenticationCommand(eq("error"), any()))
            .thenThrow(new RuntimeException("Potential exception"));
        try {
            serviceAuthenticationFilter.run();
            fail();
        } catch (ZuulRuntimeException zre) {
            assertTrue(zre.getCause() instanceof ZuulException);
            ZuulException ze = (ZuulException) zre.getCause();
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), ze.nStatusCode);
            assertEquals(String.valueOf(new RuntimeException("Potential exception")), ze.errorCause);
        }
    }

}
