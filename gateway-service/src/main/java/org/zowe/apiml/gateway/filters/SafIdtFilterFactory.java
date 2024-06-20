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
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.gateway.service.InstanceInfoService;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.ticket.TicketRequest;
import org.zowe.apiml.zaas.ZaasTokenResponse;
import reactor.core.publisher.Mono;

@Service
public class SafIdtFilterFactory extends TokenFilterFactory<SafIdtFilterFactory.Config, String> {
    private static final ObjectWriter WRITER = new ObjectMapper().writer();

    public SafIdtFilterFactory(@Qualifier("webClientClientCert") WebClient webClient, InstanceInfoService instanceInfoService, MessageService messageService) {
        super(SafIdtFilterFactory.Config.class, webClient, instanceInfoService, messageService);
    }

    @Override
    public String getEndpointUrl(ServiceInstance instance) {
        return String.format("%s://%s:%d/%s/zaas/safIdt", instance.getScheme(), instance.getHost(), instance.getPort(), instance.getServiceId().toLowerCase());
    }

    @Override
    protected Mono<Void> processResponse(ServerWebExchange exchange, GatewayFilterChain chain, AuthorizationResponse<ZaasTokenResponse> tokenResponse) {
        ServerHttpRequest request;
        var response = tokenResponse.getBody();
        if (response != null) {
            request = cleanHeadersOnAuthSuccess(exchange);
            request = request.mutate().headers(headers ->
                headers.add(ApimlConstants.SAF_TOKEN_HEADER, response.getToken())
            ).build();
            exchange = exchange.mutate().request(request).build();
        }

        return super.processResponse(exchange, chain, tokenResponse);
    }

    @Override
    protected WebClient.RequestHeadersSpec<?> createRequest(ServiceInstance instance, String requestBody) {
        String url = getEndpointUrl(instance);
        return webClient.post()
            .uri(url).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .bodyValue(requestBody);
    }

    @Override
    public GatewayFilter apply(Config config) {
        try {
            return createGatewayFilter(config, WRITER.writeValueAsString(new TicketRequest(config.getApplicationName())));
        } catch (JsonProcessingException e) {
            return ((exchange, chain) -> {
                ServerHttpRequest request = updateHeadersForError(exchange, e.getMessage());
                return chain.filter(exchange.mutate().request(request).build());
            });
        }
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Config extends TokenFilterFactory.Config {
        private String applicationName;
    }
}
