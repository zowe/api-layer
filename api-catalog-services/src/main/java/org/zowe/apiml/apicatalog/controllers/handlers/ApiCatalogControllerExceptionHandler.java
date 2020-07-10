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

import org.zowe.apiml.apicatalog.controllers.api.ApiCatalogController;
import org.zowe.apiml.apicatalog.exceptions.ContainerStatusRetrievalThrowable;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * This class creates responses for exceptional behavior of the ApiCatalogController
 */
@ControllerAdvice(assignableTypes = {ApiCatalogController.class})
@RequiredArgsConstructor
public class ApiCatalogControllerExceptionHandler {
    private final MessageService messageService;

    /**
     * Could not retrieve container details
     *
     * @param exception ContainerStatusRetrievalThrowable
     * @return 500 and the message 'Could not retrieve container statuses, {optional text}'
     */
    @ExceptionHandler(ContainerStatusRetrievalThrowable.class)
    public ResponseEntity<ApiMessageView> handleServiceNotFoundException(ContainerStatusRetrievalThrowable exception) {
        Message message = messageService.createMessage("org.zowe.apiml.apicatalog.containerStatusRetrievalException", exception.getMessage());
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(message.mapToView());
    }
}
