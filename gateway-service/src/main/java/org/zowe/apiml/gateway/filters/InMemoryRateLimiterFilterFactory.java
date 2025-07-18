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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class InMemoryRateLimiterFilterFactory extends AbstractGatewayFilterFactory<InMemoryRateLimiterFilterFactory.Config> {

    @InjectApimlLogger
    private ApimlLogger apimlLog = ApimlLogger.empty();

    private InMemoryRateLimiter rateLimiter;

    private final KeyResolver keyResolver;

    private final ObjectMapper mapper;

    private final MessageService messageService;

    public InMemoryRateLimiterFilterFactory(InMemoryRateLimiter rateLimiter, KeyResolver keyResolver, ObjectMapper mapper, MessageService messageService) {
        super(Config.class);
        this.rateLimiter = rateLimiter;
        this.keyResolver = keyResolver;
        this.mapper = mapper;
        this.messageService = messageService;
    }

    @Override
    public GatewayFilter apply(Config config) {
        this.rateLimiter.setParameters(config.capacity, config.tokens, config.refillDuration);
        return (exchange, chain) -> {
            List<PathContainer.Element> pathElements = exchange.getRequest().getPath().elements();
            String requestPath = (!pathElements.isEmpty() && pathElements.size() > 1) ? pathElements.get(1).value() : null;
            if (requestPath == null) {
                return chain.filter(exchange);
            }
            return keyResolver.resolve(exchange).flatMap(key -> {
                if (key.isEmpty()) {
                    return chain.filter(exchange);
                }
                return rateLimiter.isAllowed(requestPath, key).flatMap(response -> {
                    if (response.isAllowed()) {
                        return chain.filter(exchange);
                    } else {
                        apimlLog.log("org.zowe.apiml.gateway.connectionsLimitApproached", "Connections limit exceeded for service '{}'", requestPath);
                        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                        Message message = messageService.createMessage("org.zowe.apiml.gateway.connectionsLimitApproached", "Connections limit exceeded for service '{}'", requestPath);
                        try {
                            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(mapper.writeValueAsBytes(message.mapToView()))));
                        } catch (JsonProcessingException e) {
                            apimlLog.log("org.zowe.apiml.security.errorWrittingResponse", e.getMessage());
                            return Mono.error(e);
                        }
                    }
                });
            });
        };
    }

    @Getter
    @Setter
    public static class Config {
        private int capacity;
        private int tokens;
        private int refillDuration;
    }
}
