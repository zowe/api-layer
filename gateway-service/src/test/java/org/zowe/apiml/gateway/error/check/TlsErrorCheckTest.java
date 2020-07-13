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


import org.zowe.apiml.gateway.error.ErrorUtils;
import org.zowe.apiml.gateway.error.InternalServerErrorController;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageService;
import com.netflix.zuul.exception.ZuulException;
import com.netflix.zuul.monitoring.MonitoringHelper;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.net.ssl.SSLHandshakeException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TlsErrorCheckTest {
    private static final String TEST_MESSAGE = "Hello";
    private static InternalServerErrorController errorController;

    @BeforeAll
    static void setup() {
        MonitoringHelper.initMocks();
        MessageService messageService = new YamlMessageService();
        errorController = new InternalServerErrorController(messageService);
    }

    @Test
    void testZuulHandshakeException() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        ZuulException exc = new ZuulException(new SSLHandshakeException(TEST_MESSAGE),
                HttpStatus.INTERNAL_SERVER_ERROR.value(), "TEST");
        request.setAttribute(ErrorUtils.ATTR_ERROR_EXCEPTION, exc);

        ResponseEntity<ApiMessageView> response = errorController.error(request);

        assertEquals(HttpStatus.BAD_GATEWAY.value(), response.getStatusCodeValue());
        assertEquals("org.zowe.apiml.common.tlsError", response.getBody().getMessages().get(0).getMessageKey());
        assertTrue(response.getBody().getMessages().get(0).getMessageContent().contains(TEST_MESSAGE));
    }
}
