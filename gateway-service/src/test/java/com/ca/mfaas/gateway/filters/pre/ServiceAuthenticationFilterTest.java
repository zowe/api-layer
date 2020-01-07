package com.ca.mfaas.gateway.filters.pre;/*
                                         * This program and the accompanying materials are made available under the terms of the
                                         * Eclipse Public License v2.0 which accompanies this distribution, and is available at
                                         * https://www.eclipse.org/legal/epl-v20.html
                                         *
                                         * SPDX-License-Identifier: EPL-2.0
                                         *
                                         * Copyright Contributors to the Zowe Project.
                                         */

import com.ca.apiml.security.common.token.TokenAuthentication;
import com.ca.mfaas.gateway.security.service.ServiceAuthenticationServiceImpl;
import com.ca.mfaas.gateway.security.service.schema.AuthenticationCommand;
import com.netflix.zuul.context.RequestContext;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.servlet.http.HttpServletRequest;

import static org.mockito.Mockito.*;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVICE_ID_KEY;

@RunWith(MockitoJUnitRunner.class)
public class ServiceAuthenticationFilterTest {

    @Mock
    private ServiceAuthenticationServiceImpl serviceAuthenticationService;

    @InjectMocks
    private ServiceAuthenticationFilter serviceAuthenticationFilter;

    @Mock
    private AuthenticationCommand command;

    @Before
    public void init() {
        when(serviceAuthenticationService.getAuthenticationCommand(anyString(), any())).thenReturn(command);
    }

    @Test
    @Ignore("TODO: Pavel")
    public void testRun() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getUserPrincipal()).thenReturn(new TokenAuthentication("user", "token"));
        RequestContext requestContext = mock(RequestContext.class);
        when(requestContext.getRequest()).thenReturn(request);
        when(requestContext.get(SERVICE_ID_KEY)).thenReturn("service");
        RequestContext.testSetCurrentContext(requestContext);

        serviceAuthenticationFilter.run();
        verify(serviceAuthenticationService, times(1)).getAuthenticationCommand("service", "token");
        verify(command, times(1)).apply(null);

        when(request.getUserPrincipal()).thenReturn(() -> null);
        serviceAuthenticationFilter.run();
        verify(serviceAuthenticationService, times(1)).getAuthenticationCommand("service", null);
        verify(command, times(2)).apply(null);
    }

}
