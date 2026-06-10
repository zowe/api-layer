/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.zowe.apiml.cache.StorageException;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import reactor.core.publisher.Mono;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class CachingControllerAdvice {

    private final MessageService messageService;

    @ExceptionHandler(StorageException.class)
    public Mono<ResponseEntity<Object>> handleStorageException(StorageException exception) {
        log.debug("Storage exception", exception);
        Message message = messageService.createMessage(exception.getKey(), (Object[]) exception.getParameters());
        return Mono.just(ResponseEntity.status(exception.getStatus()).body(message.mapToView()));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<Object>> handleException(Exception exception) {
        log.debug("Internal error occurred", exception);
        Message message = messageService.createMessage("org.zowe.apiml.common.internalRequestError",
            exception.getMessage(), exception.toString());
        return Mono.just(ResponseEntity.status(500).body(message.mapToView()));
    }
}
