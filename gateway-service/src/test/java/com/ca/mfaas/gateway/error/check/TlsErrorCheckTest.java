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
import com.ca.mfaas.gateway.error.InternalServerErrorController;
import com.ca.mfaas.rest.response.ApiMessage;
import com.netflix.zuul.exception.ZuulException;
import com.netflix.zuul.monitoring.MonitoringHelper;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.net.ssl.SSLHandshakeException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TlsErrorCheckTest {
    private static final String TEST_MESSAGE = "Hello";
    private static InternalServerErrorController errorController;

    @BeforeClass
    public static void setup() {
        MonitoringHelper.initMocks();
        ErrorService errorService = new ErrorServiceImpl();
        errorController = new InternalServerErrorController(errorService);
    }

    @Test
    public void testZuulHandshakeException() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        ZuulException exc = new ZuulException(new SSLHandshakeException(TEST_MESSAGE),
            HttpStatus.INTERNAL_SERVER_ERROR.value(), "TEST");
        request.setAttribute(ErrorUtils.ATTR_ERROR_EXCEPTION, exc);

        ResponseEntity<ApiMessage> response = errorController.error(request);

        assertEquals(HttpStatus.BAD_GATEWAY.value(), response.getStatusCodeValue());
        assertEquals("apiml.common.tlsError", response.getBody().getMessages().get(0).getMessageKey());
        assertTrue(response.getBody().getMessages().get(0).getMessageContent().contains(TEST_MESSAGE));
    }
}
