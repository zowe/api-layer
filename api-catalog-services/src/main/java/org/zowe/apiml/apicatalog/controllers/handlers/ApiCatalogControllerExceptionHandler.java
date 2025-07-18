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
import org.zowe.apiml.apicatalog.controllers.api.ServicesController;
import org.zowe.apiml.apicatalog.exceptions.ContainerStatusRetrievalException;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import reactor.core.publisher.Mono;

import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * This class creates responses for exceptional behavior of the ApiCatalogController
 */
@Order(0)
@ControllerAdvice(assignableTypes = {ServicesController.class})
@RequiredArgsConstructor
public class ApiCatalogControllerExceptionHandler {

    private final MessageService messageService;

    /**
     * Could not retrieve container details
     *
     * @param exception ContainerStatusRetrievalThrowable
     * @return 500 and the message 'Could not retrieve container statuses, {optional text}'
     */
    @ExceptionHandler(ContainerStatusRetrievalException.class)
    public Mono<ResponseEntity<ApiMessageView>> handleServiceNotFoundException(ContainerStatusRetrievalException exception) {
        Message message = messageService.createMessage("org.zowe.apiml.apicatalog.containerStatusRetrievalException", exception.getMessage());
        return Mono.just(ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(APPLICATION_JSON)
            .body(message.mapToView()));
    }

}
