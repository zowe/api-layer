/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.apicatalog.staticapi;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.zowe.apiml.apicatalog.services.status.model.ServiceNotFoundException;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.product.constants.CoreService;

/**
 * This class creates responses for exceptional behavior of the StaticAPIRefreshController
 */
@ControllerAdvice(assignableTypes = {StaticAPIRefreshController.class})
@RequiredArgsConstructor
public class StaticAPIRefreshControllerExceptionHandler {
    private final MessageService messageService;

    /**
     * Could not initialized or find Discovery service
     *
     * @param exception ServiceNotFoundException
     * @return 503 status code
     */
    @ExceptionHandler(ServiceNotFoundException.class)
    public ResponseEntity<ApiMessageView> handleServiceNotFoundException(ServiceNotFoundException exception) {
        Message message = messageService.createMessage("org.zowe.apiml.apicatalog.serviceNotFound", CoreService.DISCOVERY.getServiceId());

        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(message.mapToView());
    }
}
