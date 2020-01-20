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

import org.springframework.http.HttpStatus;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

public class ErrorUtils {
    static final String UNEXPECTED_ERROR_OCCURRED = "Unexpected error occurred";
    static final String ATTR_ERROR_STATUS_CODE = "javax.servlet.error.status_code";
    public static final String ATTR_ERROR_EXCEPTION = "javax.servlet.error.exception";

    private ErrorUtils() {}

    public static int getErrorStatus(HttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute(ATTR_ERROR_STATUS_CODE);
        return statusCode != null ? statusCode : HttpStatus.INTERNAL_SERVER_ERROR.value();
    }

    public static String getErrorMessage(HttpServletRequest request) {
        final Throwable exc = (Throwable) request.getAttribute(ATTR_ERROR_EXCEPTION);
        return exc != null ? exc.getMessage() : UNEXPECTED_ERROR_OCCURRED;
    }

    public static String getGatewayUri(HttpServletRequest request) {
        return (String) request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI);
    }
}
