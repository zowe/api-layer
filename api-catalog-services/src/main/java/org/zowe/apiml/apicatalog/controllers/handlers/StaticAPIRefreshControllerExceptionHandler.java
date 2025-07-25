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
import org.springframework.web.client.RestClientException;
import org.zowe.apiml.apicatalog.controllers.api.StaticAPIRefreshController;
import org.zowe.apiml.apicatalog.exceptions.ServiceNotFoundException;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.product.constants.CoreService;
import reactor.core.publisher.Mono;

import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * This class creates responses for exceptional behavior of the StaticAPIRefreshController
 */
@Order(0)
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
    public Mono<ResponseEntity<ApiMessageView>> handleServiceNotFoundException(ServiceNotFoundException exception) {
        Message message = messageService.createMessage("org.zowe.apiml.apicatalog.serviceNotFound", CoreService.DISCOVERY.getServiceId());

        return Mono.just(ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .contentType(APPLICATION_JSON)
            .body(message.mapToView()));
    }

    /**
     * Could not handle http request
     *
     * @param exception RestClientException
     * @return 500 status code if there is any exception with refresh api
     */
    @ExceptionHandler(RestClientException.class)
    public Mono<ResponseEntity<ApiMessageView>> handleServiceNotFoundException(RestClientException exception) {
        Message message = messageService.createMessage("org.zowe.apiml.apicatalog.StaticApiRefreshFailed",
            exception);

        return Mono.just(ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(APPLICATION_JSON)
            .body(message.mapToView()));
    }
}
