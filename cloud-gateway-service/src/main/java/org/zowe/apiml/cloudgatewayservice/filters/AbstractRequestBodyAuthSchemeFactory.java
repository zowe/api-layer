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
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.zowe.apiml.cloudgatewayservice.service.InstanceInfoService;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.ticket.TicketRequest;

@Service
public abstract class AbstractRequestBodyAuthSchemeFactory<R> extends AbstractAuthSchemeFactory<AbstractRequestBodyAuthSchemeFactory.Config, R, String> {
    private static final ObjectWriter WRITER = new ObjectMapper().writer();

    public AbstractRequestBodyAuthSchemeFactory(WebClient webClient, InstanceInfoService instanceInfoService, MessageService messageService) {
        super(Config.class, webClient, instanceInfoService, messageService);
    }

    public abstract String getEndpointUrl(ServiceInstance instance);

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
    public static class Config extends AbstractAuthSchemeFactory.AbstractConfig {

        private String applicationName;

    }
}
