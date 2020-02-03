/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.error;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageService;
import org.junit.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.RequestDispatcher;

public class InternalServerErrorControllerTest {
    @Test
    public void testGenericError() {
        MessageService messageService = new YamlMessageService();
        InternalServerErrorController errorController = new InternalServerErrorController(messageService);

        MockHttpServletRequest request = new MockHttpServletRequest();

        request.setAttribute(ErrorUtils.ATTR_ERROR_EXCEPTION, new Exception("Hello"));
        request.setAttribute(ErrorUtils.ATTR_ERROR_STATUS_CODE, 523);
        request.setAttribute(RequestDispatcher.FORWARD_REQUEST_URI, "/uri");

        ResponseEntity<ApiMessageView> response = errorController.error(request);

        assertEquals(523,  response.getStatusCodeValue());
        assertEquals("apiml.common.internalRequestError",  response.getBody().getMessages().get(0).getMessageKey());
        assertTrue(response.getBody().getMessages().get(0).getMessageContent().contains("Hello"));
        assertTrue(response.getBody().getMessages().get(0).getMessageContent().contains("/uri"));
    }
}
