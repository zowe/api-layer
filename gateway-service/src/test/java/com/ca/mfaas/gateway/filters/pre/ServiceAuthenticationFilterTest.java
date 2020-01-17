/*
* This program and the accompanying materials are made available under the terms of the
* Eclipse Public License v2.0 which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Copyright Contributors to the Zowe Project.
*/
package com.ca.mfaas.gateway.filters.pre;

import com.ca.mfaas.gateway.security.service.AuthenticationService;
import com.ca.mfaas.gateway.security.service.ServiceAuthenticationServiceImpl;
import com.ca.mfaas.gateway.security.service.schema.AuthenticationCommand;
import com.ca.mfaas.gateway.utils.CurrentRequestContextTest;
import com.netflix.zuul.context.RequestContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.servlet.http.HttpServletRequest;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVICE_ID_KEY;

@RunWith(MockitoJUnitRunner.class)
public class ServiceAuthenticationFilterTest extends CurrentRequestContextTest {

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
    }

}
