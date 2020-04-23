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
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.zowe.apiml.gateway.config.MessageServiceConfiguration;
import org.zowe.apiml.gateway.error.ErrorUtils;
import org.zowe.apiml.gateway.error.InternalServerErrorController;
import org.zowe.apiml.gateway.ribbon.http.RequestAbortException;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.MessageService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {MessageServiceConfiguration.class})
class RibbonRetryErrorCheckTest {

    private static InternalServerErrorController underTest;

    @Autowired
    private MessageService messageService;

    @BeforeAll
    public static void setupAll() {
        MonitoringHelper.initMocks();
    }

    @BeforeEach
    public void setup() {
        underTest = new InternalServerErrorController(messageService);
    }

    @Test
    void givenExceptionChain_whenIsAbortException_thenRequestAbortedGeneric() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        ZuulException exc = new ZuulException(new Exception(new RequestAbortException("test")), HttpStatus.INTERNAL_SERVER_ERROR.value(), "");
        request.setAttribute(ErrorUtils.ATTR_ERROR_EXCEPTION, exc);
        ResponseEntity<ApiMessageView> response = underTest.error(request);

        assertCorrectResponse(response,
            "The request to the URL 'null' has been aborted without retrying on another instance. Caused by: null",
            HttpStatus.INTERNAL_SERVER_ERROR, "org.zowe.apiml.gateway.requestAborted");
    }

    @Test
    void givenExceptionChain_whenIsAbortExceptionWithCause_thenRequestAbortedGenericAndCause() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        ZuulException exc = new ZuulException(new Exception(new RequestAbortException(new AuthorizationServiceException("test"))), HttpStatus.INTERNAL_SERVER_ERROR.value(), "");
        request.setAttribute(ErrorUtils.ATTR_ERROR_EXCEPTION, exc);
        ResponseEntity<ApiMessageView> response = underTest.error(request);

        assertCorrectResponse(response,
            "The request to the URL 'null' has been aborted without retrying on another instance. Caused by: org.springframework.security.access.AuthorizationServiceException: test",
            HttpStatus.INTERNAL_SERVER_ERROR, "org.zowe.apiml.gateway.requestAborted");
    }

    private void assertCorrectResponse(ResponseEntity<ApiMessageView> response, String expectedMessage, HttpStatus expectedStatus, String expectedKey) {
        assertThat(response.getStatusCodeValue(), is(expectedStatus.value()));
        assertThat(response.getBody().getMessages().get(0).getMessageKey(), is(expectedKey));
        assertThat(response.getBody().getMessages().get(0).getMessageContent(), containsString(expectedMessage));
    }
}
