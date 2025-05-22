/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.handler;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

@RestControllerAdvice
public class GlobalWebFluxExceptionHandler {

    @ExceptionHandler(ServerWebInputException.class)
    public Mono<ResponseEntity<ErrorInfo>> handleDeserialization(ServerWebInputException ex) {
        System.out.println(ex);
        System.out.println(ex.getCause());
        Throwable rootCause = getRootCause(ex);
        return Mono.just(ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorInfo("Failed to deserialize the request body", rootCause.getMessage())));
    }

    private Throwable getRootCause(Throwable ex) {
        Throwable cause = ex;
        while (cause.getCause() != null && cause != cause.getCause()) {
            cause = cause.getCause();
        }
        return cause;
    }

    @Data
    @AllArgsConstructor
    static class ErrorInfo {
        private String error;
        private String exception;
    }
}
