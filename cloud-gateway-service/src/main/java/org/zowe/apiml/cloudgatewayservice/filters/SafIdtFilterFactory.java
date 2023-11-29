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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.zowe.apiml.cloudgatewayservice.service.InstanceInfoService;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.ticket.TicketRequest;
import org.zowe.apiml.zaas.ZaasTokenResponse;
import reactor.core.publisher.Mono;

import java.net.HttpCookie;

@Service
public class SafIdtFilterFactory extends AbstractRequestBodyAuthSchemeFactory<ZaasTokenResponse> {
    private static final String SAF_TOKEN_HEADER = "X-SAF-Token";

    public SafIdtFilterFactory(@Qualifier("webClientClientCert") WebClient webClient, InstanceInfoService instanceInfoService, MessageService messageService) {
        super(Config.class, webClient, instanceInfoService, messageService);
    }

    @Override
    protected WebClient.RequestHeadersSpec<?> createRequest(ServiceInstance instance, String requestBody) {
        String tokensUrl = String.format("%s://%s:%d/%s/zaas/safIdt", instance.getScheme(), instance.getHost(), instance.getPort(), instance.getServiceId().toLowerCase());
        return webClient.post()
            .uri(tokensUrl).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .bodyValue(requestBody);
    }

    @Override
    protected Mono<Void> processResponse(ServerWebExchange exchange, GatewayFilterChain chain, ZaasTokenResponse response) {
        ServerHttpRequest request;
        if (response.getToken() != null) {
            request = exchange.getRequest().mutate().headers(headers ->
                headers.add(SAF_TOKEN_HEADER, response.getToken())
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
