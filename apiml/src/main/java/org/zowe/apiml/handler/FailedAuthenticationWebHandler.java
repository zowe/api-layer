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

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import org.zowe.apiml.security.common.error.AuthExceptionHandler;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

@Slf4j
@RequiredArgsConstructor
@Component
public class FailedAuthenticationWebHandler implements ServerAuthenticationFailureHandler {

    private final ObjectMapper mapper;
    private final AuthExceptionHandler handler;

    @InjectApimlLogger
    private final ApimlLogger apimlLog = ApimlLogger.empty();

    @Override
    public Mono<Void> onAuthenticationFailure(WebFilterExchange webFilterExchange, AuthenticationException exception) {
        var exchange = webFilterExchange.getExchange();
        var requestUri = exchange.getRequest().getURI().getPath();
        log.debug("Unauthorized access to '{}' endpoint", requestUri);
        var bufferFactory = new DefaultDataBufferFactory();
        AtomicReference<DefaultDataBuffer> buffer = new AtomicReference<>();
        BiConsumer<ApiMessageView, HttpStatus> consumer = (message, status) -> {
            exchange.getResponse().setStatusCode(status);
            if (message != null) {
                exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                try {
                    buffer.set(bufferFactory.wrap(mapper.writeValueAsBytes(message)));
                } catch (IOException e) {
                    apimlLog.log("org.zowe.apiml.security.errorWrittingResponse", e.getMessage());
                }
            } else {
                buffer.set(bufferFactory.wrap(new byte[0]));
            }
        };
        var addHeader = (BiConsumer<String, String>)(name, value) -> exchange.getResponse().getHeaders().add(name, value);
        try {
            handler.handleException(requestUri, consumer, addHeader, exception);
        } catch (ServletException e) {
            // This should never happen in modulith mode, but handler declares ServletException in its signature
            throw new RuntimeException(e);
        }
        DataBuffer dataBuffer = buffer.get();
        if (dataBuffer != null) {
            return exchange.getResponse().writeWith(Mono.just(dataBuffer));
        } else {
            return exchange.getResponse().setComplete(); // avoids NPE
        }

    }

}
