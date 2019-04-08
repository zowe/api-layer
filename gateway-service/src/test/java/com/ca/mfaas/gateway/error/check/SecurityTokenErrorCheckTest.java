/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package com.ca.mfaas.gateway.error.check;

import com.ca.mfaas.error.ErrorService;
import com.ca.mfaas.error.impl.ErrorServiceImpl;
import com.ca.mfaas.gateway.error.ErrorUtils;
import com.ca.mfaas.gateway.security.token.TokenExpireException;
import com.netflix.zuul.exception.ZuulException;
import com.netflix.zuul.monitoring.MonitoringHelper;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.AuthenticationException;

import static org.mockito.Mockito.mock;

public class SecurityTokenErrorCheckTest {

    private static SecurityTokenErrorCheck securityTokenErrorCheck;

    @BeforeClass
    public static void setup() {
        MonitoringHelper.initMocks();
        ErrorService errorService = new ErrorServiceImpl();
        securityTokenErrorCheck = new SecurityTokenErrorCheck(errorService);
    }
    @Test
    public void shouldReturnUnauthorizedStatus() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        AuthenticationException exception = mock(AuthenticationException.class);
        ZuulException exc = new ZuulException(exception, HttpStatus.GATEWAY_TIMEOUT.value(), null);
        ResponseEntity response = securityTokenErrorCheck.checkError(request, exc);
        Assert.assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    public void should() {

        MockHttpServletRequest request = new MockHttpServletRequest();
        TokenExpireException tokenExpireException = new TokenExpireException("error");
//        AuthenticationException exception = mock(AuthenticationException.class);
        AuthenticationException exception = new AuthenticationException("errir", tokenExpireException) {
        };

        System.out.println(exception.getCause());
        ZuulException exc = new ZuulException(exception, HttpStatus.GATEWAY_TIMEOUT.value(), String.valueOf(exception.getCause()));
        System.out.println(exc.getCause());
        request.setAttribute(ErrorUtils.ATTR_ERROR_EXCEPTION, exc);
        ResponseEntity response = securityTokenErrorCheck.checkError(request, exc);
        Assert.assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}
