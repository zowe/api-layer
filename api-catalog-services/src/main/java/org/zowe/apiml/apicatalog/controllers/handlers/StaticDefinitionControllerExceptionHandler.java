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
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.zowe.apiml.apicatalog.controllers.api.StaticDefinitionController;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;

import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * This class creates responses for exceptional behavior of the StaticDefinitionController
 */
@Slf4j
@Order(0)
@ControllerAdvice(assignableTypes = {StaticDefinitionController.class})
@RequiredArgsConstructor
public class StaticDefinitionControllerExceptionHandler {
    private final MessageService messageService;

    /**
     * Could not create the static definition file
     *
     * @param exception IOException
     * @return 500 status code
     */
    @ExceptionHandler(IOException.class)
    public Mono<ResponseEntity<ApiMessageView>> handleIOException(IOException exception) {
        log.error("Cannot write the static definition file because: {}", exception.getMessage());
        Message message = messageService.createMessage("org.zowe.apiml.apicatalog.StaticDefinitionGenerationFailed",
            exception);

        return Mono.just(ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(APPLICATION_JSON)
            .body(message.mapToView()));
    }

    /**
     * Handle the exception when trying to override the file
     *
     * @param exception FileAlreadyExistsException
     * @return 409 status code
     */
    @ExceptionHandler(FileAlreadyExistsException.class)
    public Mono<ResponseEntity<ApiMessageView>> handleFileAlreadyExistsException(FileAlreadyExistsException exception) {
        Message message = messageService.createMessage("org.zowe.apiml.apicatalog.StaticDefinitionGenerationFailed",
            exception);

        return Mono.just(ResponseEntity
            .status(HttpStatus.CONFLICT)
            .contentType(APPLICATION_JSON)
            .body(message.mapToView()));
    }
}
