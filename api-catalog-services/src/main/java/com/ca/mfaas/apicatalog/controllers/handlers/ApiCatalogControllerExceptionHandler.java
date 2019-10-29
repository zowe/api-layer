/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.controllers.handlers;

import com.ca.mfaas.apicatalog.controllers.api.ApiCatalogController;
import com.ca.mfaas.apicatalog.exceptions.ContainerStatusRetrievalException;
import com.ca.mfaas.message.api.ApiMessageView;
import com.ca.mfaas.message.core.Message;
import com.ca.mfaas.message.core.MessageService;
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
     * @param exception ContainerStatusRetrievalException
     * @return 500 and the message 'Could not retrieve container statuses, {optional text}'
     */
    @ExceptionHandler(ContainerStatusRetrievalException.class)
    public ResponseEntity<ApiMessageView> handleServiceNotFoundException(ContainerStatusRetrievalException exception) {
        Message message = messageService.createMessage("com.ca.mfaas.caapicatalog.containerStatusRetrievalException", exception.getMessage());
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(message.mapToView());
    }
}
