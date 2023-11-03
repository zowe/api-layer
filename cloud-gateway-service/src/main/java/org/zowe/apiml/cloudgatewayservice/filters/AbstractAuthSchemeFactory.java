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

import lombok.Data;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.zowe.apiml.cloudgatewayservice.service.InstanceInfoService;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.message.core.MessageService;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Function;

public abstract class AbstractAuthSchemeFactory<T, R, D> extends AbstractGatewayFilterFactory<T> {

    private static final String HEADER_SERVICE_ID = "X-Service-Id";

    protected final WebClient webClient;
    protected final InstanceInfoService instanceInfoService;
    protected final MessageService messageService;

    protected AbstractAuthSchemeFactory(Class<T> configClazz, WebClient webClient, InstanceInfoService instanceInfoService, MessageService messageService) {
        super(configClazz);
        this.webClient = webClient;
        this.instanceInfoService = instanceInfoService;
        this.messageService = messageService;
    }

    protected abstract Class<R> getResponseClass();

    private Mono<List<ServiceInstance>> getZaasInstances() {
        return instanceInfoService.getServiceInstance("gateway");
    }

    protected Mono<Void> invoke(
        List<ServiceInstance> serviceInstances,
        AbstractConfig config,
        Function<ServiceInstance, WebClient.RequestHeadersSpec<?>> requestCreator,
        Function<? super R, ? extends Mono<Void>> responseProcessor
    ) {
        RuntimeException latestException = null;

        for (ServiceInstance instance : serviceInstances) {
            try {
                return requestCreator.apply(instance)
                    .header(HEADER_SERVICE_ID, config.serviceId)
                    .retrieve()
                    .onStatus(HttpStatus::is4xxClientError, response -> Mono.empty())
                    .bodyToMono(getResponseClass())
                    .flatMap(responseProcessor);

            } catch (RuntimeException re) {
                latestException = re;
            }
        }

        if (latestException != null) {
            throw latestException;
        }

        throw new IllegalArgumentException("No ZAAS is available");
    }

    @SuppressWarnings("squid:S1452")
    protected abstract WebClient.RequestHeadersSpec<?> createRequest(ServerWebExchange exchange, ServiceInstance instance, D data);
    protected abstract Mono<Void> processResponse(ServerWebExchange exchange, GatewayFilterChain chain, R response);

    protected GatewayFilter createGatewayFilter(AbstractConfig config, D data) {
        return (ServerWebExchange exchange, GatewayFilterChain chain) ->
            getZaasInstances().flatMap(instances  -> invoke(
                instances,
                config,
                instance -> createRequest(exchange, instance, data),
                response -> processResponse(exchange, chain, response)
                ));
    }
    protected ServerHttpRequest addRequestHeader(ServerWebExchange exchange, String key, String value) {
        return exchange.getRequest().mutate()
            .headers(headers -> headers.add(key, value))
            .build();
    }

    protected ServerHttpRequest setRequestHeader(ServerWebExchange exchange, String headerName, String headerValue) {
        return exchange.getRequest().mutate()
            .header(headerName, headerValue)
            .build();
    }

    protected ServerHttpRequest updateHeadersForError(ServerWebExchange exchange, String errorMessage) {
        ServerHttpRequest request = addRequestHeader(exchange, ApimlConstants.AUTH_FAIL_HEADER, messageService.createMessage("org.zowe.apiml.security.ticket.generateFailed", errorMessage).mapToLogMessage());
        exchange.getResponse().getHeaders().add(ApimlConstants.AUTH_FAIL_HEADER, messageService.createMessage("org.zowe.apiml.security.ticket.generateFailed", errorMessage).mapToLogMessage());
        return request;
    }

    @Data
    protected abstract static class AbstractConfig {

        private String serviceId;

    }

}
