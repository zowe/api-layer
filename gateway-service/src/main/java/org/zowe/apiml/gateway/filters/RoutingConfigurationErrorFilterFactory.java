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

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.gateway.service.InstanceInfoService;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.product.opentelemetry.OtelRequestContext;
import reactor.core.publisher.Mono;

import java.util.function.Function;

@Service
public class RoutingConfigurationErrorFilterFactory extends AbstractAuthSchemeFactory<RoutingConfigurationErrorFilterFactory.Config, Object> {

    public RoutingConfigurationErrorFilterFactory(InstanceInfoService instanceInfoService, MessageService messageService) {
        super(Config.class, instanceInfoService, messageService);
    }

    @Override
    protected Function<RequestCredentials, Mono<AuthorizationResponse<Object>>> getAuthorizationResponseTransformer() {
        throw new IllegalStateException("not implemented");
    }

    @Override
    protected Mono<Void> processResponse(ServerWebExchange clientCallBuilder, GatewayFilterChain chain, AuthorizationResponse<Object> response) {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public GatewayFilter apply(Config config) {
        var authenticationScheme = AuthenticationScheme.fromString(config.getAuthenticationScheme());

        return ((exchange, chain) -> {
            OtelRequestContext.of(exchange)
                .authMethod(authenticationScheme);

            super.cleanHeadersOnAuthFail(exchange, config.getMessage());
            return chain.filter(exchange);
        });
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Config extends AbstractAuthSchemeFactory.AbstractConfig {

        private String authenticationScheme;
        private String message;

    }

}
