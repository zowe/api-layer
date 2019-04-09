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
import com.ca.mfaas.gateway.security.token.TokenNotValidException;
import com.netflix.zuul.exception.ZuulException;
import com.netflix.zuul.monitoring.MonitoringHelper;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.AuthenticationException;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
        AuthenticationException exception = new TokenExpireException("TOKEN_EXPIRE");
        ZuulException exc = new ZuulException(exception, HttpStatus.GATEWAY_TIMEOUT.value(), null);

        ResponseEntity response = securityTokenErrorCheck.checkError(request, exc);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    public void shouldReturnCauseMessageWhenTokenExpireException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        TokenExpireException tokenExpireException = new TokenExpireException("TOKEN_EXPIRE");

        AuthenticationException exception = new TokenExpireException(tokenExpireException.getMessage(), tokenExpireException);

        ZuulException exc = new ZuulException(exception, HttpStatus.GATEWAY_TIMEOUT.value(), String.valueOf(exception));

        request.setAttribute(ErrorUtils.ATTR_ERROR_EXCEPTION, exc);
        ResponseEntity response = securityTokenErrorCheck.checkError(request, exc);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

        String basicApiMessage = "Internal error: Invalid message key 'com.ca.mfaas.security.tokenIsExpiredWithoutUrl' is provided. Please contact support for further assistance.";
        assertTrue(response.getBody().toString().contains(basicApiMessage));
    }

    @Test
    public void shouldReturnCauseMessageWhenTokenNotValidException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        TokenNotValidException tokenNotValidException = new TokenNotValidException("TOKEN_NOT_VALID");

        AuthenticationException exception = new TokenNotValidException(tokenNotValidException.getMessage(), tokenNotValidException);

        ZuulException exc = new ZuulException(exception, HttpStatus.GATEWAY_TIMEOUT.value(), String.valueOf(exception));

        request.setAttribute(ErrorUtils.ATTR_ERROR_EXCEPTION, exc);
        ResponseEntity response = securityTokenErrorCheck.checkError(request, exc);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

        String basicApiMessage = "Internal error: Invalid message key 'com.ca.mfaas.security.tokenIsNotValidWithoutUrl' is provided. Please contact support for further assistance.";
        assertTrue(response.getBody().toString().contains(basicApiMessage));
    }
}

