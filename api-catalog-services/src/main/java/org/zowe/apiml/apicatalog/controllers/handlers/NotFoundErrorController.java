/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.controllers.handlers;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.apache.http.HttpStatus;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.zowe.apiml.product.compatibility.ApimlErrorController;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

/**
 * Handles errors in REST API processing.
 */
@Controller
@Order(Ordered.HIGHEST_PRECEDENCE)
public class NotFoundErrorController implements ApimlErrorController {

    private static final String PATH = "/not_found";    // NOSONAR

    @GetMapping(value = "/not_found")
    @HystrixCommand
    public String handleError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        if (status != null) {
            switch (Integer.valueOf(status.toString())) {
            case HttpStatus.SC_NOT_FOUND:
                return "redirect:/#/not_found";
            case HttpStatus.SC_INTERNAL_SERVER_ERROR:
                return "error-500";
            default:
                return "error";
            }
        }
        return "error";
    }

    @Override
    public String getErrorPath() {
        return PATH;
    }
}
