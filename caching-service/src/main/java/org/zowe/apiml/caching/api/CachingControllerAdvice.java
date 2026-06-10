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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;
import org.zowe.apiml.cache.CacheNotAvailableException;
import org.zowe.apiml.cache.DuplicateKeyException;
import org.zowe.apiml.cache.IncompatibleStorageMethodException;
import org.zowe.apiml.cache.InsufficientStorageException;
import org.zowe.apiml.cache.InvalidPayloadException;
import org.zowe.apiml.cache.KeyNotFoundException;
import org.zowe.apiml.cache.KeyNotProvidedException;
import org.zowe.apiml.cache.StorageException;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import reactor.core.publisher.Mono;

@Slf4j
@RestControllerAdvice(assignableTypes = {CachingController.class})
@RequiredArgsConstructor
public class CachingControllerAdvice {

    private final MessageService messageService;

    @ExceptionHandler(KeyNotFoundException.class)
    public Mono<ResponseEntity<Object>> handleKeyNotFound(ServerWebExchange exchange, KeyNotFoundException ex) {
        log.debug("Key not found on {}: {}", exchange.getRequest().getURI(), ex.getMessage());
        return exceptionToMonoResponse(ex);
    }

    @ExceptionHandler(KeyNotProvidedException.class)
    public Mono<ResponseEntity<Object>> handleKeyNotProvided(ServerWebExchange exchange, KeyNotProvidedException ex) {
        log.debug("Key not provided on {}: {}", exchange.getRequest().getURI(), ex.getMessage());
        return exceptionToMonoResponse(ex);
    }

    @ExceptionHandler(InvalidPayloadException.class)
    public Mono<ResponseEntity<Object>> handleInvalidPayload(ServerWebExchange exchange, InvalidPayloadException ex) {
        log.debug("Invalid payload on {}: {}", exchange.getRequest().getURI(), ex.getMessage());
        return exceptionToMonoResponse(ex);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public Mono<ResponseEntity<Object>> handleDuplicateKey(ServerWebExchange exchange, DuplicateKeyException ex) {
        log.debug("Duplicate key on {}: {}", exchange.getRequest().getURI(), ex.getMessage());
        return exceptionToMonoResponse(ex);
    }

    @ExceptionHandler(CacheNotAvailableException.class)
    public Mono<ResponseEntity<Object>> handleCacheNotAvailable(ServerWebExchange exchange, CacheNotAvailableException ex) {
        log.debug("Cache not available on {}: {}", exchange.getRequest().getURI(), ex.getMessage());
        return exceptionToMonoResponse(ex);
    }

    @ExceptionHandler(InsufficientStorageException.class)
    public Mono<ResponseEntity<Object>> handleInsufficientStorage(ServerWebExchange exchange, InsufficientStorageException ex) {
        log.debug("Insufficient storage on {}: {}", exchange.getRequest().getURI(), ex.getMessage());
        return exceptionToMonoResponse(ex);
    }

    @ExceptionHandler(IncompatibleStorageMethodException.class)
    public Mono<ResponseEntity<Object>> handleIncompatibleStorageMethod(ServerWebExchange exchange, IncompatibleStorageMethodException ex) {
        log.debug("Incompatible storage method on {}: {}", exchange.getRequest().getURI(), ex.getMessage());
        return exceptionToMonoResponse(ex);
    }

    @ExceptionHandler(StorageException.class)
    public Mono<ResponseEntity<Object>> handleStorageException(ServerWebExchange exchange, StorageException ex) {
        log.debug("Storage exception on {}: {}", exchange.getRequest().getURI(), ex.getMessage());
        return exceptionToMonoResponse(ex);
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<Object>> handleInternal(ServerWebExchange exchange, Exception ex) {
        log.debug("Internal error on {}: {}", exchange.getRequest().getURI(), ex.getMessage());
        Message message = messageService.createMessage(
            "org.zowe.apiml.common.internalServerError", ex.getMessage());
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(message.mapToView()));
    }

    private Mono<ResponseEntity<Object>> exceptionToMonoResponse(StorageException ex) {
        Message message = messageService.createMessage(ex.getKey(), (Object[]) ex.getParameters());
        return Mono.just(ResponseEntity.status(ex.getStatus()).body(message.mapToView()));
    }
}
