/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * This filter checks if encoded slashes in the URI are allowed based on configuration.
 * If not allowed and encoded slashes are present, it returns a BAD_REQUEST response.
 */
@Component
@RequiredArgsConstructor
//@ConditionalOnProperty(value = "apiml.service.allowEncodedSlashes", havingValue = "true")
public class TomcatFilter implements GatewayFilter {

    private final MessageService messageService;
    private final ObjectMapper mapper;

    @InjectApimlLogger
    private final ApimlLogger apimlLog = ApimlLogger.empty();

    /**
     * Filters requests to check for encoded slashes in the URI.
     * If encoded slashes are not allowed and found, returns a BAD_REQUEST response.
     * Otherwise, proceeds with the filter chain.
     *
     * @param exchange The server web exchange
     * @param chain    The web filter chain
     * @return A Mono that indicates when request processing is complete
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String uri = exchange.getRequest().getURI().toString();

        // Use CompletableFuture to handle decoding asynchronously
        CompletableFuture<String> decodedUriFuture = CompletableFuture.supplyAsync(() -> URLDecoder.decode(uri, StandardCharsets.UTF_8));

        return Mono.fromFuture(decodedUriFuture).flatMap(decodedUri -> {
            final boolean isRequestEncoded = !uri.equals(decodedUri);

            Message message = messageService.createMessage("org.zowe.apiml.gateway.requestContainEncodedSlash", uri);
            if (isRequestEncoded) {
                exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
                exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                return exchange.getResponse().writeWith(Mono.create(sink -> {
                    try {
                        sink.success(exchange.getResponse()
                            .bufferFactory()
                            .wrap(mapper.writeValueAsBytes(message.mapToView())));
                    } catch (IOException e) {
                        apimlLog.log("org.zowe.apiml.security.errorWritingResponse", e.getMessage());
                        sink.error(new RuntimeException("Error writing response", e));
                    }
                }));
            } else {
                return chain.filter(exchange);
            }
        });
    }
}
