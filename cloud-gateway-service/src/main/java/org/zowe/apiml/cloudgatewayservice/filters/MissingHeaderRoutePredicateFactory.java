/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.filters;

import java.util.function.Predicate;

import javax.validation.constraints.NotEmpty;

import lombok.Getter;
import org.springframework.cloud.gateway.handler.predicate.AbstractRoutePredicateFactory;
import org.springframework.cloud.gateway.handler.predicate.GatewayPredicate;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ServerWebExchange;

/**
 * This predicate is responsible for checking if the request header is missing. If the header is missing, routing
 * will continue.
 * It is used in org.zowe.apiml.cloudgatewayservice.service.routing.ByBasePath to prevent routing if the header is set.
 */
@Service
public class MissingHeaderRoutePredicateFactory extends AbstractRoutePredicateFactory<MissingHeaderRoutePredicateFactory.Config> {

    public MissingHeaderRoutePredicateFactory() {
        super(Config.class);
    }

    @Override
    public Predicate<ServerWebExchange> apply(Config config) {
        return new GatewayPredicate() {
            @Override
            public boolean test(ServerWebExchange exchange) {
                return !exchange.getRequest().getHeaders().containsKey(config.header);
            }

            @Override
            public Object getConfig() {
                return config;
            }

            @Override
            public String toString() {
                return String.format("Missing header: %s", config.header);
            }
        };
    }

    @Getter
    @Validated
    public static class Config {

        @NotEmpty
        private String header;

        public Config setHeader(String header) {
            this.header = header;
            return this;
        }

    }

}
