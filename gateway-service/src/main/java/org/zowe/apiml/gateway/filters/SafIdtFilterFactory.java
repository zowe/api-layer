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
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import org.zowe.apiml.gateway.service.InstanceInfoService;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.zaas.ZaasTokenResponse;
import reactor.core.publisher.Mono;

import java.util.function.Function;

@Service
public class SafIdtFilterFactory extends AbstractTokenFilterFactory<SafIdtFilterFactory.Config> {

    private final ZaasSchemeTransform zaasSchemeTransform;

    public SafIdtFilterFactory(ZaasSchemeTransform zaasSchemeTransform, InstanceInfoService instanceInfoService, MessageService messageService) {
        super(SafIdtFilterFactory.Config.class, instanceInfoService, messageService);
        this.zaasSchemeTransform = zaasSchemeTransform;
    }

    @Override
    protected Function<RequestCredentials, Mono<AuthorizationResponse<ZaasTokenResponse>>> getAuthorizationResponseTransformer() {
        return zaasSchemeTransform::safIdt;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return createGatewayFilter(config);
    }

    @Override
    protected RequestCredentials.RequestCredentialsBuilder createRequestCredentials(ServerWebExchange exchange, Config config) {
        return super.createRequestCredentials(exchange, config)
            .applId(config.getApplicationName());
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Config extends AbstractTokenFilterFactory.Config {
        private String applicationName;
    }

}
