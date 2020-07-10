/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.error.check;

import org.springframework.context.annotation.Import;
import org.zowe.apiml.config.error.check.MessageServiceConfiguration;
import org.zowe.apiml.security.common.token.TokenExpireException;
import org.zowe.apiml.security.common.token.TokenNotValidException;
import org.zowe.apiml.gateway.error.ErrorUtils;
import org.zowe.apiml.message.api.ApiMessage;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.core.MessageType;
import com.netflix.zuul.exception.ZuulException;
import com.netflix.zuul.monitoring.MonitoringHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.AuthenticationException;
import org.springframework.test.context.junit.jupiter.SpringExtension;


import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@ExtendWith(SpringExtension.class)
@Import(MessageServiceConfiguration.class)
class SecurityTokenErrorCheckTest {

    private static SecurityTokenErrorCheck securityTokenErrorCheck;

    @Autowired
    private MessageService messageService;

    @BeforeAll
    static void initMocks() {
        MonitoringHelper.initMocks();
    }

    @BeforeEach
    void setup() {
        securityTokenErrorCheck = new SecurityTokenErrorCheck(messageService);
    }


    @Test
    void shouldReturnCauseMessageWhenTokenExpireException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        TokenExpireException tokenExpireException = new TokenExpireException("TOKEN_EXPIRE");

        AuthenticationException exception = new TokenExpireException(tokenExpireException.getMessage(), tokenExpireException);

        ZuulException exc = new ZuulException(exception, HttpStatus.GATEWAY_TIMEOUT.value(), String.valueOf(exception));

        request.setAttribute(ErrorUtils.ATTR_ERROR_EXCEPTION, exc);
        ResponseEntity<ApiMessageView> actualResponse = securityTokenErrorCheck.checkError(request, exc);

        assertNotNull(actualResponse);
        assertEquals(HttpStatus.UNAUTHORIZED, actualResponse.getStatusCode());
        List<ApiMessage> actualMessageList = actualResponse.getBody().getMessages();
        assertThat(actualMessageList, hasItem(new ApiMessage<>("org.zowe.apiml.gateway.security.expiredToken", MessageType.ERROR, "ZWEAG103E", "Token is expired")));
    }

    @Test
    void shouldReturnCauseMessageWhenTokenNotValidException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        TokenNotValidException tokenNotValidException = new TokenNotValidException("TOKEN_NOT_VALID");

        AuthenticationException exception = new TokenNotValidException(tokenNotValidException.getMessage(), tokenNotValidException);

        ZuulException exc = new ZuulException(exception, HttpStatus.GATEWAY_TIMEOUT.value(), String.valueOf(exception));

        request.setAttribute(ErrorUtils.ATTR_ERROR_EXCEPTION, exc);
        ResponseEntity<ApiMessageView> actualResponse = securityTokenErrorCheck.checkError(request, exc);

        assertNotNull(actualResponse);
        assertEquals(HttpStatus.UNAUTHORIZED, actualResponse.getStatusCode());

        List<ApiMessage> actualMessageList = actualResponse.getBody().getMessages();
        assertThat(actualMessageList, hasItem(new ApiMessage<>("org.zowe.apiml.gateway.security.invalidToken", MessageType.ERROR, "ZWEAG102E", "Token is not valid")));
    }
}

