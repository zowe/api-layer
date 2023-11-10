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
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import org.zowe.apiml.cloudgatewayservice.service.InstanceInfoService;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.message.core.MessageService;
import reactor.core.publisher.Mono;

import java.net.HttpCookie;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.zowe.apiml.constants.ApimlConstants.PAT_COOKIE_AUTH_NAME;
import static org.zowe.apiml.constants.ApimlConstants.PAT_HEADER_NAME;
import static org.zowe.apiml.security.SecurityUtils.COOKIE_AUTH_NAME;

public abstract class AbstractAuthSchemeFactory<T, R, D> extends AbstractGatewayFilterFactory<T> {

    private static final String HEADER_SERVICE_ID = "X-Service-Id";


    private static final Predicate<HttpCookie> CREDENTIALS_COOKIE_INPUT = cookie ->
        StringUtils.equalsIgnoreCase(cookie.getName(), PAT_COOKIE_AUTH_NAME) ||
        StringUtils.equalsIgnoreCase(cookie.getName(), COOKIE_AUTH_NAME) ||
        StringUtils.startsWithIgnoreCase(cookie.getName(), COOKIE_AUTH_NAME + ".");
    private static final Predicate<HttpCookie> CREDENTIALS_COOKIE = cookie ->
        CREDENTIALS_COOKIE_INPUT.test(cookie) ||
        StringUtils.equalsIgnoreCase(cookie.getName(), "jwtToken") ||
        StringUtils.equalsIgnoreCase(cookie.getName(), "LtpaToken2");

    private static final Predicate<String> CREDENTIALS_HEADER_INPUT = headerName ->
        StringUtils.equalsIgnoreCase(headerName, HttpHeaders.AUTHORIZATION) ||
        StringUtils.equalsIgnoreCase(headerName, PAT_HEADER_NAME);
    private static final Predicate<String> CREDENTIALS_HEADER = headerName ->
        CREDENTIALS_HEADER_INPUT.test(headerName) ||
        StringUtils.equalsIgnoreCase(headerName, "X-SAF-Token") ||
        StringUtils.equalsIgnoreCase(headerName, "X-Certificate-Public") ||
        StringUtils.equalsIgnoreCase(headerName, "X-Certificate-DistinguishedName") ||
        StringUtils.equalsIgnoreCase(headerName, "X-Certificate-CommonName") ||
        StringUtils.equalsIgnoreCase(headerName, HttpHeaders.COOKIE);

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
    protected abstract R getResponseFor401();

    private Mono<List<ServiceInstance>> getZaasInstances() {
        return instanceInfoService.getServiceInstance("gateway");
    }

    private Mono<R> requestWithHa(
        Iterator<ServiceInstance> serviceInstanceIterator,
        Function<ServiceInstance, WebClient.RequestHeadersSpec<?>> requestCreator
    ) {
        return requestCreator.apply(serviceInstanceIterator.next())
            .retrieve()
            .onStatus(HttpStatus::is5xxServerError, clientResponse -> Mono.empty())
            .bodyToMono(getResponseClass())
            .onErrorResume(exception -> exception instanceof WebClientResponseException.Unauthorized ? Mono.just(getResponseFor401()) : Mono.error(exception))
            .switchIfEmpty(serviceInstanceIterator.hasNext() ?
                requestWithHa(serviceInstanceIterator, requestCreator) : Mono.empty()
            );
    }

    protected Mono<Void> invoke(
        List<ServiceInstance> serviceInstances,
        Function<ServiceInstance, WebClient.RequestHeadersSpec<?>> requestCreator,
        Function<? super R, ? extends Mono<Void>> responseProcessor
    ) {
        Iterator<ServiceInstance> i = robinRound.getIterator(serviceInstances);
        if (!i.hasNext()) {
            throw new IllegalArgumentException("No ZAAS is available");
        }

        return requestWithHa(i, requestCreator).flatMap(responseProcessor);
    }

    @SuppressWarnings("squid:S1452")
    protected abstract WebClient.RequestHeadersSpec<?> createRequest(ServiceInstance instance, D data);
    protected abstract Mono<Void> processResponse(ServerWebExchange clientCallBuilder, GatewayFilterChain chain, R response);

    protected WebClient.RequestHeadersSpec<?> createRequest(AbstractConfig config, ServerHttpRequest.Builder clientRequestbuilder, ServiceInstance instance, D data) {
        WebClient.RequestHeadersSpec<?> zaasCallBuilder = createRequest(instance, data);

        clientRequestbuilder
            .headers(headers -> {
                // get all current cookies
                List<HttpCookie> cookies = readCookies(headers).collect(Collectors.toList());

                // set in the request to ZAAS all cookies and headers that contain credentials
                headers.entrySet().stream()
                    .filter(e -> CREDENTIALS_HEADER_INPUT.test(e.getKey()))
                    .forEach(e -> zaasCallBuilder.header(e.getKey(), e.getValue().toArray(new String[0])));
                cookies.stream()
                    .filter(CREDENTIALS_COOKIE_INPUT)
                    .forEach(c -> zaasCallBuilder.cookie(c.getName(), c.getValue()));

                // add common headers to ZAAS
                zaasCallBuilder.header(HEADER_SERVICE_ID, config.serviceId);

                // update original request - to remove all potential headers and cookies with credentials
                Stream<Map.Entry<String, String>> nonCredentialHeaders = headers.entrySet().stream()
                    .filter(entry -> !CREDENTIALS_HEADER.test(entry.getKey()))
                    .flatMap(entry -> entry.getValue().stream().map(v -> new AbstractMap.SimpleEntry<>(entry.getKey(), v)));
                Stream<Map.Entry<String, String>> nonCredentialCookies = cookies.stream()
                    .filter(c -> !CREDENTIALS_COOKIE.test(c))
                    .map(c -> new AbstractMap.SimpleEntry<>(HttpHeaders.COOKIE, c.toString()));

                List<Map.Entry<String, String>> newHeaders = Stream.concat(
                    nonCredentialHeaders,
                    nonCredentialCookies
                ).collect(Collectors.toList());

                headers.clear();
                newHeaders.forEach(newHeader -> headers.add(newHeader.getKey(), newHeader.getValue()));
            });

        return zaasCallBuilder;
    }

    protected GatewayFilter createGatewayFilter(AbstractConfig config, D data) {
        return (exchange, chain) -> getZaasInstances().flatMap(
            instances -> {
                ServerHttpRequest.Builder clientCallBuilder = exchange.getRequest().mutate();
                return invoke(
                    instances,
                    instance -> createRequest(config, clientCallBuilder, instance, data),
                    response -> processResponse(exchange.mutate().request(clientCallBuilder.build()).build(), chain, response)
                );
            }
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

    protected Stream<HttpCookie> readCookies(HttpHeaders httpHeaders) {
        return Optional.ofNullable(httpHeaders.get(HttpHeaders.COOKIE))
            .orElse(Collections.emptyList())
            .stream()
            .map(v -> StringUtils.split(v, ";"))
            .flatMap(Arrays::stream)
            .map(StringUtils::trim)
            .map(HttpCookie::parse)
            .flatMap(List::stream);
    }

    protected ServerHttpRequest setCookie(ServerWebExchange exchange, String cookieName, String value) {
        return exchange.getRequest().mutate()
            .headers(headers -> {
                // read all other current cookies
                List<HttpCookie> cookies = readCookies(headers)
                    .filter(c -> !StringUtils.equals(c.getName(), cookieName))
                    .collect(Collectors.toList());

                // add the new cookie
                cookies.add(new HttpCookie(cookieName, value));

                // remove old cookie header in the request
                headers.remove(HttpHeaders.COOKIE);

                // set new cookie header in the request
                cookies.stream().forEach(c -> headers.add(HttpHeaders.COOKIE, c.toString()));
            }).build();
    }

    @Data
    protected abstract static class AbstractConfig {

        private String serviceId;

    }

}
