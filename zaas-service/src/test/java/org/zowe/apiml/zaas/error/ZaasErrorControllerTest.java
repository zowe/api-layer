/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.error;


import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageService;
import org.zowe.apiml.zaas.error.controllers.ZaasErrorController;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZaasErrorControllerTest {

    @Test
    void testGenericError() {
        MessageService messageService = new YamlMessageService();
        ZaasErrorController errorController = new ZaasErrorController(messageService);

        MockHttpServletRequest request = new MockHttpServletRequest();

        request.setAttribute(ErrorUtils.ATTR_ERROR_EXCEPTION, new Exception("Hello"));
        request.setAttribute(ErrorUtils.ATTR_ERROR_STATUS_CODE, 523);
        request.setAttribute(RequestDispatcher.FORWARD_REQUEST_URI, "/uri");

        ResponseEntity<ApiMessageView> response = errorController.error(request);

        assertEquals("org.zowe.apiml.common.internalRequestError", response.getBody().getMessages().get(0).getMessageKey());
        assertEquals(523, response.getStatusCode().value());
        assertTrue(response.getBody().getMessages().get(0).getMessageContent().contains("Hello"));
        assertTrue(response.getBody().getMessages().get(0).getMessageContent().contains("/uri"));
    }

}
