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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.RequestDispatcher;

public class ErrorUtilsTest {
    @Test
    void testGetErrorStatus() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertEquals(500, ErrorUtils.getErrorStatus(request));

        request.setAttribute(ErrorUtils.ATTR_ERROR_STATUS_CODE, 504);
        assertEquals(504, ErrorUtils.getErrorStatus(request));
    }

    @Test
    void testGetErrorMessage() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertEquals(ErrorUtils.UNEXPECTED_ERROR_OCCURRED, ErrorUtils.getErrorMessage(request));

        request.setAttribute(ErrorUtils.ATTR_ERROR_EXCEPTION, new Exception("Hello"));
        assertEquals("Hello", ErrorUtils.getErrorMessage(request));
    }

    @Test
    void testGetGatewayUri() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertEquals(null, ErrorUtils.getGatewayUri(request));

        request.setAttribute(RequestDispatcher.FORWARD_REQUEST_URI, "/uri");
        assertEquals("/uri", ErrorUtils.getGatewayUri(request));
    }
}
