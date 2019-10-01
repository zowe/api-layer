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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.ca.mfaas.gateway.error.ErrorUtils;
import com.ca.mfaas.gateway.error.InternalServerErrorController;
import com.ca.mfaas.message.api.ApiMessageView;
import com.ca.mfaas.message.core.MessageService;
import com.ca.mfaas.message.yaml.YamlMessageService;
import com.netflix.zuul.exception.ZuulException;
import com.netflix.zuul.monitoring.MonitoringHelper;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.net.SocketTimeoutException;

public class TimeoutErrorCheckTest {
    private static final String TEST_MESSAGE = "Hello";
    private static InternalServerErrorController errorController;

    @BeforeClass
    public static void setup() {
        MonitoringHelper.initMocks();
        MessageService messageService = new YamlMessageService();
        errorController = new InternalServerErrorController(messageService);
    }

    private void assertCorrectMessage(ResponseEntity<ApiMessageView> response, String expectedMessage) {
        assertEquals(HttpStatus.GATEWAY_TIMEOUT.value(), response.getStatusCodeValue());
        assertEquals("apiml.common.serviceTimeout", response.getBody().getMessages().get(0).getMessageKey());
        assertTrue(response.getBody().getMessages().get(0).getMessageContent().contains(expectedMessage));
    }

    @Test
    public void testZuulTimeoutError() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        ZuulException exc = new ZuulException(new Exception(TEST_MESSAGE), HttpStatus.GATEWAY_TIMEOUT.value(), null);
        request.setAttribute(ErrorUtils.ATTR_ERROR_EXCEPTION, exc);
        ResponseEntity<ApiMessageView> response = errorController.error(request);

        assertCorrectMessage(response, TEST_MESSAGE);
    }

    @Test
    public void testZuulTimeoutErrorWithoutCause() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        ZuulException exc = new ZuulException("", HttpStatus.GATEWAY_TIMEOUT.value(), "TEST");
        request.setAttribute(ErrorUtils.ATTR_ERROR_EXCEPTION, exc);

        ResponseEntity<ApiMessageView> response = errorController.error(request);

        assertCorrectMessage(response, TimeoutErrorCheck.DEFAULT_MESSAGE);
    }

    @Test
    public void testZuulSocketTimeoutError() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        ZuulException exc = new ZuulException(new SocketTimeoutException(TEST_MESSAGE),
                HttpStatus.INTERNAL_SERVER_ERROR.value(), "TEST");
        request.setAttribute(ErrorUtils.ATTR_ERROR_EXCEPTION, exc);

        ResponseEntity<ApiMessageView> response = errorController.error(request);

        assertCorrectMessage(response, TEST_MESSAGE);
    }
}
