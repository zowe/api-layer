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

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class InMemoryRateLimiterFilterFactory extends AbstractGatewayFilterFactory<InMemoryRateLimiterFilterFactory.Config> {

    @InjectApimlLogger
    private final ApimlLogger apimlLog = ApimlLogger.empty();
    private final InMemoryRateLimiter rateLimiter;
    private final KeyResolver keyResolver;
    @Value(value = "${apiml.routing.servicesToLimitRequestRate}")
    List<String> serviceIds;

    public InMemoryRateLimiterFilterFactory(InMemoryRateLimiter rateLimiter, KeyResolver keyResolver) {
        super(Config.class);
        this.rateLimiter = rateLimiter;
        this.keyResolver = keyResolver;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String requestPath = exchange.getRequest().getPath().elements().get(1).value();
            if (serviceIds.contains(requestPath)) {
                return keyResolver.resolve(exchange).flatMap(key -> {
                    return rateLimiter.isAllowed(config.getRouteId(), key).flatMap(response -> {
                        if (response.isAllowed()) {
                            return chain.filter(exchange);
                        } else {
                            apimlLog.log("org.zowe.apiml.gateway.connectionsLimitApproached", "Connections limit exceeded for service '{}'", requestPath);
                            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                            exchange.getResponse().getHeaders().setContentType(MediaType.TEXT_HTML);
                            String errorHtml = "<html><body><h1>429 Too Many Requests</h1>" +
                                "<p>The connection limit for the service '" + requestPath + "' has been exceeded. Please try again later.</p>" +
                                "</body></html>";
                            byte[] bytes = errorHtml.getBytes(StandardCharsets.UTF_8);
                            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
                        }
                    });
                });
            } else {
                return chain.filter(exchange);
            }
        };
    }

    @Getter
    @Setter
    public static class Config {
        private String routeId;
        private Integer capacity;
        private Integer tokens;
        private Integer refillIntervalSeconds;
    }
}
