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
import org.zowe.apiml.ticket.TicketResponse;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class PassticketFilterFactory extends AbstractAuthSchemeFactory<PassticketFilterFactory.Config, TicketResponse, String> {

    private static final String TICKET_URL = "%s://%s:%d/%s/zaas/ticket";
    private static final ObjectWriter WRITER = new ObjectMapper().writer();

    public PassticketFilterFactory(WebClient webClient, InstanceInfoService instanceInfoService, MessageService messageService) {
        super(Config.class, webClient, instanceInfoService, messageService);
    }

    @Override
    protected Class<TicketResponse> getResponseClass() {
        return TicketResponse.class;
    }

    @Override
    protected TicketResponse getResponseFor401() {
        return new TicketResponse();
    }

    @Override
    protected WebClient.RequestHeadersSpec<?> createRequest(ServiceInstance instance, String requestBody) {
        return webClient.post()
            .uri(String.format(TICKET_URL, instance.getScheme(), instance.getHost(), instance.getPort(), instance.getServiceId().toLowerCase()))
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .bodyValue(requestBody);
    }

    @Override
    protected Mono<Void> processResponse(ServerWebExchange exchange, GatewayFilterChain chain, TicketResponse response) {
        ServerHttpRequest request;
        if (response.getTicket() != null) {
            String encodedCredentials = Base64.getEncoder().encodeToString((response.getUserId() + ":" + response.getTicket()).getBytes(StandardCharsets.UTF_8));
            final String headerValue = "Basic " + encodedCredentials;
            request = setRequestHeader(exchange, HttpHeaders.AUTHORIZATION, headerValue);
        } else {
            String message = messageService.createMessage("org.zowe.apiml.security.ticket.generateFailed", "Invalid or missing authentication").mapToLogMessage();
            request = updateHeadersForError(exchange, message);
        }

        exchange = exchange.mutate().request(request).build();
        return chain.filter(exchange);
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
    public static class Config extends AbstractAuthSchemeFactory.AbstractConfig {

        private String applicationName;

    }
}
