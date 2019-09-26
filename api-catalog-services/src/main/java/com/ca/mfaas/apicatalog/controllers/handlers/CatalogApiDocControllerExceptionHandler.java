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

import com.ca.mfaas.apicatalog.controllers.api.CatalogApiDocController;
import com.ca.mfaas.apicatalog.services.status.model.ApiDocNotFoundException;
import com.ca.mfaas.apicatalog.services.status.model.ServiceNotFoundException;
import com.ca.mfaas.message.api.ApiMessageView;
import com.ca.mfaas.message.core.Message;
import com.ca.mfaas.message.core.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * This class creates responses for exceptional behavior of the CatalogApiDocController
 */
@Slf4j
@ControllerAdvice(assignableTypes = {CatalogApiDocController.class})
@RequiredArgsConstructor
public class CatalogApiDocControllerExceptionHandler {
    private final MessageService messageService;

    /**
     * Could not retrieve the API Documentation
     *
     * @param exception InvalidFormatException
     * @return 500 and the message 'TBD'
     */
    @ExceptionHandler(ApiDocNotFoundException.class)
    public ResponseEntity<ApiMessageView> handleApiDocNotFoundException(ApiDocNotFoundException exception) {
        Message message = messageService.createMessage("com.ca.mfaas.caapicatalog.apiDocNotFound", exception.getMessage());

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(message.mapToView());
    }

    /**
     * Could not retrieve the API Documentation as the Gateway was not available
     *
     * @param exception InvalidFormatException
     * @return 404 and the message 'TBD'
     */
    @ExceptionHandler(ServiceNotFoundException.class)
    public ResponseEntity<ApiMessageView> handleServiceNotFoundException(ServiceNotFoundException exception) {

        Message message = messageService.createMessage("com.ca.mfaas.caapicatalog.serviceNotFound", exception.getMessage());

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(message.mapToView());
    }
}
