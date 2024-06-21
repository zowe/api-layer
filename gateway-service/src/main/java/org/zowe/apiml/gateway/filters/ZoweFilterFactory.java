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

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.zowe.apiml.gateway.service.InstanceInfoService;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.zaas.ZaasTokenResponse;
import reactor.core.publisher.Mono;


@Service
public class ZoweFilterFactory extends TokenFilterFactory<TokenFilterFactory.Config, Object> {

    @Value("${apiml.security.auth.jwt.customAuthHeader:}")
    private String customHeader;

    public ZoweFilterFactory(@Qualifier("webClientClientCert") WebClient webClient, InstanceInfoService instanceInfoService, MessageService messageService) {
        super(TokenFilterFactory.Config.class, webClient, instanceInfoService, messageService);
    }

    @Override
    public String getEndpointUrl(ServiceInstance instance) {
        return String.format("%s://%s:%d/%s/zaas/zoweJwt", instance.getScheme(), instance.getHost(), instance.getPort(), instance.getServiceId().toLowerCase());
    }

    @Override
    protected Mono<Void> processResponse(ServerWebExchange exchange, GatewayFilterChain chain, AuthorizationResponse<ZaasTokenResponse> tokenResponse) {
        var response = tokenResponse.getBody();
        if (StringUtils.isNotEmpty(customHeader) && response != null) {
            var request = exchange.getRequest().mutate().headers(headers -> headers.add(customHeader, response.getToken())).build();
            exchange = exchange.mutate().request(request).build();
        }

        return super.processResponse(exchange, chain, tokenResponse);
    }

}
