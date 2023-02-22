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
import org.apache.http.HttpHeaders;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.zowe.apiml.cloudgatewayservice.service.InstanceInfoService;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.ticket.TicketRequest;
import org.zowe.apiml.ticket.TicketResponse;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class PassticketFilterFactory extends AbstractGatewayFilterFactory<PassticketFilterFactory.Config> {
    private final WebClient webClient;
    private final InstanceInfoService instanceInfoService;
    private final MessageService messageService;
    private final String ticketUrl = "%s://%s:%s/%s/api/v1/auth/ticket";
    private final ObjectWriter writer = new ObjectMapper().writer();

    public PassticketFilterFactory(WebClient webClient, InstanceInfoService instanceInfoService, MessageService messageService) {
        super(Config.class);
        this.webClient = webClient;
        this.instanceInfoService = instanceInfoService;
        this.messageService = messageService;
    }

    @Override
    public GatewayFilter apply(Config config) {
        try {
            final String requestBody = writer.writeValueAsString(new TicketRequest(config.getApplicationName()));
            return (ServerWebExchange exchange, GatewayFilterChain chain) ->
                instanceInfoService.getServiceInstance("gateway").flatMap(instances -> {
                    for (ServiceInstance instance : instances) {
                        return webClient.post()
                            .uri(String.format(ticketUrl, instance.getScheme(), instance.getHost(), instance.getPort(), instance.getServiceId().toLowerCase()))
                            .headers(headers -> headers.addAll(exchange.getRequest().getHeaders()))
                            .bodyValue(requestBody)
                            .retrieve().onStatus(HttpStatus::is4xxClientError, (response) -> Mono.empty())
                            .bodyToMono(TicketResponse.class)
                            .flatMap(response -> {
                                if (response.getTicket() == null) {
                                    ServerHttpRequest request = updateHeadersForError(exchange, "Invalid or missing authentication.");
                                    return chain.filter(exchange.mutate().request(request).build());
                                }
                                String encodedCredentials = Base64.getEncoder().encodeToString((response.getUserId() + ":" + response.getTicket()).getBytes(StandardCharsets.UTF_8));
                                final String headerValue = "Basic " + encodedCredentials;
                                ServerHttpRequest request = addRequestHeader(exchange, HttpHeaders.AUTHORIZATION, headerValue);
                                return chain.filter(exchange.mutate().request(request).build());
                            });
                    }
                    ServerHttpRequest request = updateHeadersForError(exchange, "All gateway service instances failed to respond.");
                    return chain.filter(exchange.mutate().request(request).build());
                });
        } catch (JsonProcessingException e) {
            return ((exchange, chain) -> {
                ServerHttpRequest request = updateHeadersForError(exchange, e.getMessage());
                return chain.filter(exchange.mutate().request(request).build());
            });
        }
    }

    private ServerHttpRequest updateHeadersForError(ServerWebExchange exchange, String errorMessage) {
        ServerHttpRequest request = addRequestHeader(exchange, ApimlConstants.AUTH_FAIL_HEADER, messageService.createMessage("org.zowe.apiml.security.ticket.generateFailed", errorMessage).mapToLogMessage());
        exchange.getResponse().getHeaders().add(ApimlConstants.AUTH_FAIL_HEADER, messageService.createMessage("org.zowe.apiml.security.ticket.generateFailed", errorMessage).mapToLogMessage());
        return request;
    }

    private ServerHttpRequest addRequestHeader(ServerWebExchange exchange, String key, String value) {
        return exchange.getRequest().mutate()
            .headers(headers -> {
                    headers.add(key, value);
                    headers.remove(org.springframework.http.HttpHeaders.COOKIE);
                }
            ).build();
    }


    public static class Config {
        private String applicationName;

        public String getApplicationName() {
            return applicationName;
        }

        public void setApplicationName(String applicationName) {
            this.applicationName = applicationName;
        }
    }
}
