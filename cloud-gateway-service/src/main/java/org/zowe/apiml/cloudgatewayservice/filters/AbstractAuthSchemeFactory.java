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
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.zowe.apiml.cloudgatewayservice.service.InstanceInfoService;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.message.core.MessageService;
import reactor.core.publisher.Mono;

import java.net.HttpCookie;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class AbstractAuthSchemeFactory<T, R, D> extends AbstractGatewayFilterFactory<T> {

    private static final String HEADER_SERVICE_ID = "X-Service-Id";

    private static final RobinRoundIterator<ServiceInstance> robinRound = new RobinRoundIterator<>();

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

    private Mono<R> requestWithHa(
        Iterator<ServiceInstance> serviceInstanceIterator,
        AbstractConfig config,
        Function<ServiceInstance, WebClient.RequestHeadersSpec<?>> requestCreator
    ) {
        return requestCreator.apply(serviceInstanceIterator.next())
            .retrieve()
            .onStatus(HttpStatus::isError, clientResponse -> Mono.empty())
            .bodyToMono(getResponseClass())
            .switchIfEmpty(serviceInstanceIterator.hasNext() ?
                requestWithHa(serviceInstanceIterator, config, requestCreator) : Mono.empty()
            );
    }

    protected Mono<Void> invoke(
        List<ServiceInstance> serviceInstances,
        AbstractConfig config,
        Function<ServiceInstance, WebClient.RequestHeadersSpec<?>> requestCreator,
        Function<? super R, ? extends Mono<Void>> responseProcessor
    ) {
        Iterator<ServiceInstance> i = robinRound.getIterator(serviceInstances);
        if (!i.hasNext()) {
            throw new IllegalArgumentException("No ZAAS is available");
        }

        return requestWithHa(i, config, requestCreator).flatMap(responseProcessor);
    }

    @SuppressWarnings("squid:S1452")
    protected abstract WebClient.RequestHeadersSpec<?> createRequest(ServerWebExchange exchange, ServiceInstance instance, D data);
    protected abstract Mono<Void> processResponse(ServerWebExchange exchange, GatewayFilterChain chain, R response);

    protected WebClient.RequestHeadersSpec<?> setDefaults(AbstractConfig config, ServerWebExchange exchange, WebClient.RequestHeadersSpec<?> requestHeadersSpec) {
        return requestHeadersSpec
            .headers(headers -> headers.addAll(exchange.getRequest().getHeaders()))
            .header(HEADER_SERVICE_ID, config.serviceId);
    }

    protected GatewayFilter createGatewayFilter(AbstractConfig config, D data) {
        return (exchange, chain) -> getZaasInstances().flatMap(
            instances -> invoke(
                instances,
                config,
                instance -> setDefaults(config, exchange, createRequest(exchange, instance, data)),
                response -> processResponse(exchange, chain, response)
            )
        );
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

    protected List<HttpCookie> readCookies(HttpHeaders headers) {
        List<HttpCookie> cookies = new LinkedList<>();
        Optional.ofNullable(headers.get(HttpHeaders.COOKIE))
            .ifPresent(cookieHeaders -> cookieHeaders.forEach(cookieHeader ->
                cookies.addAll(HttpCookie.parse(cookieHeader))));
        return cookies;
    }

    protected ServerHttpRequest setCookie(ServerWebExchange exchange, String cookieName, String value) {
        return exchange.getRequest().mutate()
            .headers(headers -> {
                // read all current cookies
                List<HttpCookie> cookies = readCookies(headers);
                // remove old on with the same name if exists
                cookies = cookies.stream().filter(c -> !StringUtils.equals(c.getName(), cookieName)).collect(Collectors.toList());
                // add the new cookie
                cookies.add(new HttpCookie(cookieName, value));

                // remove old cookie header in the request
                headers.remove(HttpHeaders.COOKIE);
                // set new cookie header in the request
                headers.set(HttpHeaders.COOKIE, cookies.stream().map(HttpCookie::toString).collect(Collectors.joining("; ")));
            }).build();
    }

    @Data
    protected abstract static class AbstractConfig {

        private String serviceId;

    }

}
