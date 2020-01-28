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


import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

/**
 * Not found endpoint controller
 */
@Controller
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class NotFoundErrorController implements ErrorController {
    private static final String NOT_FOUND_ENDPOINT = "/not_found";
    private final MessageService messageService;

    @Override
    public String getErrorPath() {
        return NOT_FOUND_ENDPOINT;
    }

    /**
     * Not found endpoint controller
     * Creates response and logs the error
     *
     * @param request Http request
     * @return Http response entity
     */
    @GetMapping(value = NOT_FOUND_ENDPOINT, produces = "application/json")
    @ResponseBody
    public ResponseEntity<ApiMessageView> notFound400HttpResponse(HttpServletRequest request) {
        Message message = messageService.createMessage("org.zowe.apiml.common.endPointNotFound",
            ErrorUtils.getGatewayUri(request));
        return ResponseEntity.status(ErrorUtils.getErrorStatus(request)).body(message.mapToView());
    }
}
