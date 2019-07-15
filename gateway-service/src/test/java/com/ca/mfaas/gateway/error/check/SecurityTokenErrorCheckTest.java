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
import com.ca.apiml.security.token.TokenExpireException;
import com.ca.apiml.security.token.TokenNotValidException;
import com.ca.mfaas.rest.response.ApiMessage;
import com.ca.mfaas.rest.response.Message;
import com.ca.mfaas.rest.response.MessageType;
import com.ca.mfaas.rest.response.impl.BasicMessage;
import com.netflix.zuul.exception.ZuulException;
import com.netflix.zuul.monitoring.MonitoringHelper;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.AuthenticationException;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Objects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
public class SecurityTokenErrorCheckTest {

    private static SecurityTokenErrorCheck securityTokenErrorCheck;

    @Autowired
    private ErrorService errorService;

    @BeforeClass
    public static void initMocks() {
        MonitoringHelper.initMocks();
    }

    @Before
    public void setup() {
        securityTokenErrorCheck = new SecurityTokenErrorCheck(errorService);
    }


    @Test
    public void shouldReturnCauseMessageWhenTokenExpireException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        TokenExpireException tokenExpireException = new TokenExpireException("TOKEN_EXPIRE");

        AuthenticationException exception = new TokenExpireException(tokenExpireException.getMessage(), tokenExpireException);

        ZuulException exc = new ZuulException(exception, HttpStatus.GATEWAY_TIMEOUT.value(), String.valueOf(exception));

        request.setAttribute(ErrorUtils.ATTR_ERROR_EXCEPTION, exc);
        ResponseEntity<ApiMessage> actualResponse = securityTokenErrorCheck.checkError(request, exc);
        assertEquals(HttpStatus.UNAUTHORIZED, actualResponse.getStatusCode());

        List<Message> actualMessageList = Objects.requireNonNull(actualResponse.getBody()).getMessages();
        assertThat(actualMessageList, hasItem(new BasicMessage("apiml.gateway.security.expiredToken", MessageType.ERROR, "ZWEAG103E", "Token is expired")));
    }

    @Test
    public void shouldReturnCauseMessageWhenTokenNotValidException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        TokenNotValidException tokenNotValidException = new TokenNotValidException("TOKEN_NOT_VALID");

        AuthenticationException exception = new TokenNotValidException(tokenNotValidException.getMessage(), tokenNotValidException);

        ZuulException exc = new ZuulException(exception, HttpStatus.GATEWAY_TIMEOUT.value(), String.valueOf(exception));

        request.setAttribute(ErrorUtils.ATTR_ERROR_EXCEPTION, exc);
        ResponseEntity<ApiMessage> actualResponse = securityTokenErrorCheck.checkError(request, exc);
        assertEquals(HttpStatus.UNAUTHORIZED, actualResponse.getStatusCode());

        List<Message> actualMessageList = Objects.requireNonNull(actualResponse.getBody()).getMessages();
        assertThat(actualMessageList, hasItem(new BasicMessage("apiml.gateway.security.invalidToken", MessageType.ERROR, "ZWEAG102E", "Token is not valid")));
    }

    @Configuration
    static class ContextConfiguration {

        @Bean
        public ErrorService errorService() {
            return new ErrorServiceImpl("/gateway-messages.yml");
        }
    }
}

