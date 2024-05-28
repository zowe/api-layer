/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.error.check;

import com.netflix.zuul.exception.ZuulException;
import com.netflix.zuul.monitoring.MonitoringHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.HttpClientErrorException;
import org.zowe.apiml.config.error.check.MessageServiceConfiguration;
import org.zowe.apiml.zaas.error.ErrorUtils;
import org.zowe.apiml.zaas.security.service.saf.SafIdtAuthException;
import org.zowe.apiml.zaas.security.service.saf.SafIdtException;
import org.zowe.apiml.message.api.ApiMessage;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.core.MessageType;
import org.zowe.apiml.security.common.token.TokenExpireException;
import org.zowe.apiml.security.common.token.TokenNotValidException;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@Import(MessageServiceConfiguration.class)
class SecurityErrorCheckTest {

    private static SecurityErrorCheck securityErrorCheck;

    private final MockHttpServletRequest request = new MockHttpServletRequest();

    @Autowired
    private MessageService messageService;

    @BeforeAll
    static void initMocks() {
        MonitoringHelper.initMocks();
    }

    @BeforeEach
    void setup() {
        securityErrorCheck = new SecurityErrorCheck(messageService);
    }


    @Test
    void shouldReturnCauseMessageWhenTokenExpireException() {
        TokenExpireException tokenExpireException = new TokenExpireException("TOKEN_EXPIRE");

        AuthenticationException exception = new TokenExpireException(tokenExpireException.getMessage(), tokenExpireException);

        ZuulException exc = new ZuulException(exception, HttpStatus.GATEWAY_TIMEOUT.value(), String.valueOf(exception));

        request.setAttribute(ErrorUtils.ATTR_ERROR_EXCEPTION, exc);
        ResponseEntity<ApiMessageView> actualResponse = securityErrorCheck.checkError(request, exc);

        assertNotNull(actualResponse);
        assertEquals(HttpStatus.UNAUTHORIZED, actualResponse.getStatusCode());
        List<ApiMessage> actualMessageList = actualResponse.getBody().getMessages();
        assertThat(actualMessageList, hasItem(new ApiMessage("org.zowe.apiml.zaas.security.expiredToken", MessageType.ERROR, "ZWEAG103E", "The token has expired", "Obtain new token by performing an authentication request.", "The JWT token has expired.")));
    }

    @Test
    void shouldReturnCauseMessageWhenTokenNotValidException() {
        TokenNotValidException tokenNotValidException = new TokenNotValidException("TOKEN_NOT_VALID");

        AuthenticationException exception = new TokenNotValidException(tokenNotValidException.getMessage(), tokenNotValidException);

        ZuulException exc = new ZuulException(exception, HttpStatus.GATEWAY_TIMEOUT.value(), String.valueOf(exception));

        request.setAttribute(ErrorUtils.ATTR_ERROR_EXCEPTION, exc);
        ResponseEntity<ApiMessageView> actualResponse = securityErrorCheck.checkError(request, exc);

        assertNotNull(actualResponse);
        assertEquals(HttpStatus.UNAUTHORIZED, actualResponse.getStatusCode());

        assertNotNull(actualResponse.getBody());
        List<ApiMessage> actualMessageList = actualResponse.getBody().getMessages();
        assertThat(actualMessageList, hasItem(new ApiMessage("org.zowe.apiml.zaas.security.invalidToken", MessageType.ERROR, "ZWEAG102E", "Token is not valid", "Provide a valid token.", "The JWT token is not valid.")));
    }

    @Test
    void shouldReturnCauseMessageWhenBadCredentialsException() {
        BadCredentialsException badCredentialsException = new BadCredentialsException("CREDENTIALS_NOT_VALID");

        AuthenticationException exception = new BadCredentialsException(badCredentialsException.getMessage(), badCredentialsException);

        ZuulException exc = new ZuulException(exception, HttpStatus.GATEWAY_TIMEOUT.value(), String.valueOf(exception));

        request.setAttribute(ErrorUtils.ATTR_ERROR_EXCEPTION, exc);
        ResponseEntity<ApiMessageView> actualResponse = securityErrorCheck.checkError(request, exc);

        assertNotNull(actualResponse);
        assertEquals(HttpStatus.UNAUTHORIZED, actualResponse.getStatusCode());

        assertNotNull(actualResponse.getBody());
        List<ApiMessage> actualMessageList = actualResponse.getBody().getMessages();
        assertThat(actualMessageList, hasItem(new ApiMessage("org.zowe.apiml.security.login.invalidCredentials",
                MessageType.ERROR, "ZWEAG120E",
                "Invalid username or password for URL 'null'",
                "Provide a valid username and password.",
                "The username and/or password are invalid."
        )));
    }

    @Test
    void shouldReturnCauseMessageWhenSafIdtAuthException() {
        String exceptionMessage = "ZSS auth failed";

        HttpClientErrorException serviceException = new HttpClientErrorException(HttpStatus.UNAUTHORIZED);
        AuthenticationException exception = new SafIdtAuthException(exceptionMessage, serviceException);
        ZuulException exc = new ZuulException(exception, 8, exception.getLocalizedMessage());

        ResponseEntity<ApiMessageView> actualResponse = securityErrorCheck.checkError(request, exc);

        assertNotNull(actualResponse);
        assertEquals(HttpStatus.UNAUTHORIZED, actualResponse.getStatusCode());

        assertNotNull(actualResponse.getBody());
        List<ApiMessage> actualMessageList = actualResponse.getBody().getMessages();
        assertThat(actualMessageList, hasItem(new ApiMessage(
                "org.zowe.apiml.security.idt.auth.failed",
                MessageType.ERROR, "ZWEAG151E",
                "SAF IDT is not generated because authentication or authorization failed. Reason: " + exceptionMessage + ". " + exception.getCause().getLocalizedMessage(),
                "Provide a valid username and password.",
                "The user credentials were rejected during SAF verification. Review the reason in the error message."
        )));

    }

    @Test
    void shouldReturnCauseMessageWhenSafIdtException() {
        String exceptionMessage = "ZSS failed";

        AccessDeniedException exception = new SafIdtException(exceptionMessage);
        ZuulException exc = new ZuulException(exception, 8, exception.getLocalizedMessage());

        ResponseEntity<ApiMessageView> actualResponse = securityErrorCheck.checkError(request, exc);

        assertNotNull(actualResponse);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, actualResponse.getStatusCode());

        assertNotNull(actualResponse.getBody());
        List<ApiMessage> actualMessageList = actualResponse.getBody().getMessages();
        assertThat(actualMessageList, hasItem(new ApiMessage(
                "org.zowe.apiml.security.idt.failed",
                MessageType.ERROR, "ZWEAG150E",
                "SAF IDT generation failed. Reason: " + exceptionMessage + ". ",
                "Verify the Identity Token configuration.",
                "An error occurred during SAF verification. Review the reason in the error message."
        )));
    }

}

