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

import jakarta.servlet.ServletException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ServerWebExchange;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.security.common.error.AuthExceptionHandler;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class DefaultExceptionHandler {

    private final AuthExceptionHandler authExceptionHandler;

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ApiMessageView>> handleException(ServerWebExchange exchange, Exception exception) {
        AtomicReference<ResponseEntity<ApiMessageView>> responseJson = new AtomicReference<>();
        BiConsumer<ApiMessageView, HttpStatus> consumer = (message, status) ->
            responseJson.set(ResponseEntity
                .status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(message)
            );

        try {
            authExceptionHandler.handleException(exchange.getRequest().getPath().value(), consumer, exchange.getResponse().getHeaders()::add, exception);
            return Mono.just(responseJson.get());
        } catch (ServletException e) {
            log.error("Cannot handle exception: {}", exception, e);
            return Mono.error(() -> new RuntimeException(e));
        }
    }

}
