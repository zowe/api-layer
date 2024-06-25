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
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.gateway.service.InstanceInfoService;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.ticket.TicketRequest;
import org.zowe.apiml.ticket.TicketResponse;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

@Service
public class PassticketFilterFactory extends AbstractAuthSchemeFactory<PassticketFilterFactory.Config, TicketResponse, String> {
    private static final ObjectWriter WRITER = new ObjectMapper().writer();

    private static final String TICKET_URL = "%s://%s:%d/%s/zaas/ticket";

    @Value("${apiml.security.auth.passticket.customUserHeader:}")
    private String customUserHeader;

    @Value("${apiml.security.auth.passticket.customAuthHeader:}")
    private String customPassTicketHeader;

    public PassticketFilterFactory(@Qualifier("webClientClientCert") WebClient webClient, InstanceInfoService instanceInfoService, MessageService messageService) {
        super(Config.class, webClient, instanceInfoService, messageService);
    }

    @Override
    protected Class<TicketResponse> getResponseClass() {
        return TicketResponse.class;
    }

    @Override
    protected Mono<Void> processResponse(ServerWebExchange exchange, GatewayFilterChain chain, AuthorizationResponse<TicketResponse> ticketResponse) {
        ServerHttpRequest request;
        var response = ticketResponse.getBody();
        if (response != null) {
            request = cleanHeadersOnAuthSuccess(exchange);

            String encodedCredentials = Base64.getEncoder().encodeToString((response.getUserId() + ":" + response.getTicket()).getBytes(StandardCharsets.UTF_8));

            var requestSpec = request.mutate();
            requestSpec = requestSpec.header(HttpHeaders.AUTHORIZATION, "Basic " + encodedCredentials);
            if (StringUtils.isNotEmpty(customUserHeader) && StringUtils.isNotEmpty(customPassTicketHeader)) {
                requestSpec = requestSpec.header(customUserHeader, response.getUserId());
                requestSpec = requestSpec.header(customPassTicketHeader, response.getTicket());
            }
            request = requestSpec.build();
        } else {
            var oidcToken = Optional.ofNullable(ticketResponse.getHeaders())
                .map(ClientResponse.Headers::asHttpHeaders)
                .map(httpHeaders -> httpHeaders.getFirst(ApimlConstants.HEADER_OIDC_TOKEN));
            String failureHeader = Optional.of(ticketResponse)
                .map(AuthorizationResponse::getHeaders)
                .map(headers -> headers.header(ApimlConstants.AUTH_FAIL_HEADER.toLowerCase()))
                .filter(list -> !list.isEmpty())
                .map(list -> list.get(0))
                .orElse(messageService.createMessage("org.zowe.apiml.security.ticket.generateFailed", "Invalid or missing authentication").mapToLogMessage());
            if (oidcToken.isPresent()) {
                request = cleanHeadersOnAuthSuccess(exchange);
                //In case ZAAS will return 401, and there is OIDC token that used for authentication. See use case with valid OIDC token, but missing user mapping.
                request = request.mutate().headers(httpHeaders -> {
                    httpHeaders.add(ApimlConstants.HEADER_OIDC_TOKEN, oidcToken.get());
                    httpHeaders.add(ApimlConstants.AUTH_FAIL_HEADER, failureHeader);
                }).build();
                exchange.getResponse().getHeaders().add(ApimlConstants.AUTH_FAIL_HEADER, failureHeader);
            } else {
                request = cleanHeadersOnAuthFail(exchange, failureHeader);
            }
        }

        exchange = exchange.mutate().request(request).build();
        return chain.filter(exchange);
    }

    @Override
    protected WebClient.RequestHeadersSpec<?> createRequest(ServiceInstance instance, String requestBody) {
        String url = String.format(TICKET_URL, instance.getScheme(), instance.getHost(), instance.getPort(), instance.getServiceId().toLowerCase());
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
    public static class Config extends AbstractAuthSchemeFactory.AbstractConfig {
        private String applicationName;
    }

}
