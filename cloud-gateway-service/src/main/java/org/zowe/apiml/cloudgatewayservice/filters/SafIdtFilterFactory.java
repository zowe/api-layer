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

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.zowe.apiml.cloudgatewayservice.service.InstanceInfoService;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.zaas.ZaasTokenResponse;
import reactor.core.publisher.Mono;

@Service
public class SafIdtFilterFactory extends AbstractRequestBodyAuthSchemeFactory<ZaasTokenResponse> {

    public SafIdtFilterFactory(@Qualifier("webClientClientCert") WebClient webClient, InstanceInfoService instanceInfoService, MessageService messageService) {
        super(webClient, instanceInfoService, messageService);
    }

    @Override
    public String getEndpointUrl(ServiceInstance instance) {
        return String.format("%s://%s:%d/%s/zaas/safIdt", instance.getScheme(), instance.getHost(), instance.getPort(), instance.getServiceId().toLowerCase());
    }

    @Override
    protected Mono<Void> processResponse(ServerWebExchange exchange, GatewayFilterChain chain, ZaasTokenResponse response) {
        ServerHttpRequest request;
        if (response.getToken() != null) {
            request = exchange.getRequest().mutate().headers(headers ->
                headers.add(ApimlConstants.SAF_TOKEN_HEADER, response.getToken())
            ).build();
        } else {
            request = updateHeadersForError(exchange, "Invalid or missing authentication");
        }

        exchange = exchange.mutate().request(request).build();
        return chain.filter(exchange);
    }

    @Override
    protected Class<ZaasTokenResponse> getResponseClass() {
        return ZaasTokenResponse.class;
    }

    @Override
    protected ZaasTokenResponse getResponseFor401() {
        return new ZaasTokenResponse();
    }

}
