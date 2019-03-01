/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.service.gateway.error;

import com.broadcom.apiml.test.integration.error.ErrorService;
import com.broadcom.apiml.test.integration.error.impl.ErrorServiceImpl;
import com.broadcom.apiml.test.integration.rest.response.ApiMessage;
import org.junit.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.RequestDispatcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InternalServerErrorControllerTest {
    @Test
    public void testGenericError() {
        ErrorService errorService = new ErrorServiceImpl();
        InternalServerErrorController errorController = new InternalServerErrorController(errorService);

        MockHttpServletRequest request = new MockHttpServletRequest();

        request.setAttribute(ErrorUtils.ATTR_ERROR_EXCEPTION, new Exception("Hello"));
        request.setAttribute(ErrorUtils.ATTR_ERROR_STATUS_CODE, 523);
        request.setAttribute(RequestDispatcher.FORWARD_REQUEST_URI, "/uri");

        ResponseEntity<ApiMessage> response = errorController.error(request);

        assertEquals(523, response.getStatusCodeValue());
        assertEquals("apiml.common.internalRequestError", response.getBody().getMessages().get(0).getMessageKey());
        assertTrue(response.getBody().getMessages().get(0).getMessageContent().contains("Hello"));
        assertTrue(response.getBody().getMessages().get(0).getMessageContent().contains("/uri"));
    }
}
