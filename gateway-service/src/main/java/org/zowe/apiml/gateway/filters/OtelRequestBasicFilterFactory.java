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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.zowe.apiml.product.opentelemetry.RoutingContext;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Component
@ConditionalOnProperty(value = "otel.sdk.disabled", havingValue = "false", matchIfMissing = true)
public class OtelRequestBasicFilterFactory extends AbstractGatewayFilterFactory<OtelRequestBasicFilterFactory.Config> {

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            var otelContext = RoutingContext.of(exchange)
                .method(exchange.getRequest().getMethod())
                .scheme(exchange.getRequest().getURI().getScheme())
                .path(exchange.getRequest().getURI().getPath())
                .serviceId(config.serviceId)
                .instanceId(config.instanceId);
            return chain.filter(exchange)
                .then(Mono.fromRunnable(() -> Optional.ofNullable(exchange.getResponse())
                    .map(ServerHttpResponse::getStatusCode)
                    .map(HttpStatusCode::value)
                    .ifPresent(otelContext::responseCode)
                ))
                .then(Mono.fromRunnable(() -> RoutingContext.of(exchange).issue()));
        };
    }

    @Getter
    @Setter
    public static class Config {

        private String instanceId;
        private String serviceId;

    }

}
