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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.product.opentelemetry.OtelRequestContext;

@Component
@ConditionalOnProperty(value = "otel.sdk.disabled", havingValue = "false", matchIfMissing = true)
public class OtelServiceFilterFactory extends AbstractGatewayFilterFactory<OtelServiceFilterFactory.Config> {

    public OtelServiceFilterFactory() {
        super(OtelServiceFilterFactory.Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            var ctx = OtelRequestContext.of(exchange)
                .authMethod(AuthenticationScheme.BYPASS)
                .serviceId(config.serviceId)
                .instanceId(config.instanceId);

            if (AuthenticationScheme.BYPASS.name().equalsIgnoreCase(config.authenticationScheme)) {
                ctx.anonymousUserId()
                   .authenticationSuccess();
            }
            return chain.filter(exchange);
        };
    }

    @Data
    @Validated
    public static class Config {

        private String instanceId;
        private String serviceId;
        private String authenticationScheme;

    }

}
