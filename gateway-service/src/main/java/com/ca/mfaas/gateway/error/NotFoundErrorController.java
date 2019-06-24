/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.error;

import com.ca.mfaas.error.ErrorService;
import com.ca.mfaas.rest.response.ApiMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

@Controller
@Order(Ordered.HIGHEST_PRECEDENCE)
public class NotFoundErrorController implements ErrorController {

    private static final String NOT_FOUND_ENDPOINT = "/not_found";
    private final ErrorService errorService;

    @Autowired
    public NotFoundErrorController(ErrorService errorService) {
        this.errorService = errorService;
    }

    @Override
    public String getErrorPath() {
        return NOT_FOUND_ENDPOINT;
    }

    @GetMapping(value = NOT_FOUND_ENDPOINT, produces = "application/json")
    @ResponseBody
    public ResponseEntity<ApiMessage> notFound400HttpResponse(HttpServletRequest request) {
        ApiMessage message = errorService.createApiMessage("com.ca.mfaas.common.endPointNotFound",
                ErrorUtils.getGatewayUri(request));
        return ResponseEntity.status(ErrorUtils.getErrorStatus(request)).body(message);
    }
}
