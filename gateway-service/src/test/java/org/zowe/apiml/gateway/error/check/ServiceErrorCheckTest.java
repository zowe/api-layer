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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.zowe.apiml.gateway.config.MessageServiceConfiguration;
import org.zowe.apiml.gateway.error.ErrorUtils;
import org.zowe.apiml.gateway.error.NotFound;
import org.zowe.apiml.message.api.ApiMessage;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.core.MessageType;
import org.zowe.apiml.security.common.error.ServiceNotAccessibleException;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {MessageServiceConfiguration.class})
class ServiceErrorCheckTest {
    private ErrorCheck underTest;

    @Autowired
    private MessageService messageService;

    @BeforeEach
    void prepareCheckUnderTest() {
        MonitoringHelper.initMocks();
        underTest = new ServiceErrorCheck(messageService);
    }

    @Test
    void givenNotFoundZuulException_whenTheRequestIsProcessed_then404IsReturned() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        ZuulException exc = new ZuulException(new NotFound(), HttpStatus.NOT_FOUND.value(), "serviceId");

        request.setAttribute(ErrorUtils.ATTR_ERROR_EXCEPTION, exc);
        ResponseEntity<ApiMessageView> actualResponse = underTest.checkError(request, exc);

        assertNotNull(actualResponse);
        assertEquals(HttpStatus.NOT_FOUND, actualResponse.getStatusCode());
        List<ApiMessage> actualMessageList = actualResponse.getBody().getMessages();
        assertThat(actualMessageList, hasItem(new ApiMessage("org.zowe.apiml.common.endPointNotFound",
            MessageType.ERROR, "ZWEAM104E", "The endpoint you are looking for 'serviceId' could not be located")));
    }

    @Test
    void givenServiceNotAccessibleException_whenTheRequestIsProcessed_then404IsReturned() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        ZuulException exc = new ZuulException(new ServiceNotAccessibleException("service"), HttpStatus.NOT_FOUND.value(), "serviceId");

        request.setAttribute(ErrorUtils.ATTR_ERROR_EXCEPTION, exc);
        ResponseEntity<ApiMessageView> actualResponse = underTest.checkError(request, exc);

        assertNotNull(actualResponse);
        assertEquals(HttpStatus.NOT_FOUND, actualResponse.getStatusCode());
        List<ApiMessage> actualMessageList = actualResponse.getBody().getMessages();
        assertThat(actualMessageList, hasItem(new ApiMessage("org.zowe.apiml.gateway.serviceUnavailable",
            MessageType.ERROR, "ZWEAG709E", "Service is not available at URL 'null'. Error returned: 'null'")));
    }
}
