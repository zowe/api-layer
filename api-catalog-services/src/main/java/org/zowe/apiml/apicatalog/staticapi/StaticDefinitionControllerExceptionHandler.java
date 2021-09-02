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
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;

/**
 * This class creates responses for exceptional behavior of the StaticDefinitionController
 */
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
    public ResponseEntity<ApiMessageView> handleIOException(IOException exception) {
        Message message = messageService.createMessage("org.zowe.apiml.apicatalog.StaticDefinitionGenerationFailed",
            exception);

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(message.mapToView());
    }

    /**
     * Handle the exception when trying to override the file
     *
     * @param exception FileAlreadyExistsException
     * @return 409 status code
     */
    @ExceptionHandler(FileAlreadyExistsException.class)
    public ResponseEntity<ApiMessageView> handleFileAlreadyExistsException(FileAlreadyExistsException exception) {
        Message message = messageService.createMessage("org.zowe.apiml.apicatalog.StaticDefinitionGenerationFailed",
            exception);

        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(message.mapToView());
    }
}
