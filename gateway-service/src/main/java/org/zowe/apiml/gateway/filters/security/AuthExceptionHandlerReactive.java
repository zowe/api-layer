/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.filters.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.MessageService;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Component
@RequiredArgsConstructor
public class AuthExceptionHandlerReactive {

    private final MessageService messageService;
    private final ObjectMapper objectMapper;

    public Mono<Void> handleTokenNotValid(ServerWebExchange exchange) {
        var response = exchange.getResponse();
        response.setStatusCode(UNAUTHORIZED);
        response.getHeaders().add(ApimlConstants.AUTH_FAIL_HEADER, "Invalid token");
        response.getHeaders().add(CONTENT_TYPE, APPLICATION_JSON_VALUE);

        ApiMessageView message = messageService
            .createMessage("org.zowe.apiml.common.unauthorized", exchange.getRequest().getPath())
            .mapToView();

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(message);
        } catch (JsonProcessingException e) {
            bytes = "{\"message\":\"Invalid token\"}".getBytes(StandardCharsets.UTF_8);
        }

        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }

    public Mono<Void> handleServiceUnavailable(ServerWebExchange exchange) {
        var response = exchange.getResponse();
        response.setStatusCode(SERVICE_UNAVAILABLE);
        response.getHeaders().add(CONTENT_TYPE, APPLICATION_JSON_VALUE);

        ApiMessageView message = messageService
            .createMessage("org.zowe.apiml.common.serviceUnavailable", exchange.getRequest().getPath())
            .mapToView();

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(message);
        } catch (JsonProcessingException e) {
            bytes = "{\"message\":\"service unavailable\"}".getBytes(StandardCharsets.UTF_8);
        }

        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }
}
