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
import org.apache.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.zowe.apiml.gateway.service.InstanceInfoService;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.ticket.TicketResponse;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class PassticketFilterFactory extends AbstractRequestBodyAuthSchemeFactory<TicketResponse> {

    private static final String TICKET_URL = "%s://%s:%d/%s/zaas/ticket";

    @Value("${apiml.security.auth.passticket.customUserHeader:}")
    private String customUserHeader;

    @Value("${apiml.security.auth.passticket.customAuthHeader:}")
    private String customPassTicketHeader;

    public PassticketFilterFactory(@Qualifier("webClientClientCert") WebClient webClient, InstanceInfoService instanceInfoService, MessageService messageService) {
        super(webClient, instanceInfoService, messageService);
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
    public String getEndpointUrl(ServiceInstance instance) {
        return String.format(TICKET_URL, instance.getScheme(), instance.getHost(), instance.getPort(), instance.getServiceId().toLowerCase());
    }

    @Override
    protected Mono<Void> processResponse(ServerWebExchange exchange, GatewayFilterChain chain, TicketResponse response) {
        ServerHttpRequest request;
        if (response.getTicket() != null) {
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
            String message = messageService.createMessage("org.zowe.apiml.security.ticket.generateFailed", "Invalid or missing authentication").mapToLogMessage();
            request = cleanHeadersOnAuthFail(exchange, message);
        }

        exchange = exchange.mutate().request(request).build();
        return chain.filter(exchange);
    }

}
