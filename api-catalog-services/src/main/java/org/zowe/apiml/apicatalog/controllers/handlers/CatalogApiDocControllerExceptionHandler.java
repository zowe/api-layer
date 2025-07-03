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

import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.zowe.apiml.apicatalog.controllers.api.ApiDocController;
import org.zowe.apiml.apicatalog.exceptions.ApiDocNotFoundException;
import org.zowe.apiml.apicatalog.exceptions.ServiceNotFoundException;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import reactor.core.publisher.Mono;

import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * This class creates responses for exceptional behavior of the CatalogApiDocController
 */
@Order(0)
@ControllerAdvice(assignableTypes = {ApiDocController.class})
@RequiredArgsConstructor
public class CatalogApiDocControllerExceptionHandler {

    private final MessageService messageService;

    /**
     * Could not retrieve the API Documentation
     *
     * @param exception InvalidFormatException
     * @return 404 and the message 'API Documentation not retrieved...'
     */
    @ExceptionHandler(ApiDocNotFoundException.class)
    public Mono<ResponseEntity<ApiMessageView>> handleApiDocNotFoundException(ApiDocNotFoundException exception) {
        Message message = messageService.createMessage("org.zowe.apiml.apicatalog.apiDocNotFound", exception.getMessage());

        return Mono.just(ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .contentType(APPLICATION_JSON)
            .body(message.mapToView()));
    }

    /**
     * Could not retrieve the API Documentation as the Gateway was not available
     *
     * @param exception InvalidFormatException
     * @return 404 and the message 'Service not located...'
     */
    @ExceptionHandler(ServiceNotFoundException.class)
    public Mono<ResponseEntity<ApiMessageView>> handleServiceNotFoundException(ServiceNotFoundException exception) {
        Message message = messageService.createMessage("org.zowe.apiml.apicatalog.serviceNotFound", exception.getMessage());

        return Mono.just(ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .contentType(APPLICATION_JSON)
            .body(message.mapToView()));
    }

}
