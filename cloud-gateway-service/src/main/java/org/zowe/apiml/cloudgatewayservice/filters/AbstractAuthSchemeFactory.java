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

import static org.zowe.apiml.cloudgatewayservice.filters.ClientCertFilterFactory.CLIENT_CERT_HEADER;
import static org.zowe.apiml.constants.ApimlConstants.PAT_COOKIE_AUTH_NAME;
import static org.zowe.apiml.constants.ApimlConstants.PAT_HEADER_NAME;
import static org.zowe.apiml.security.SecurityUtils.COOKIE_AUTH_NAME;

/**
 * This class is responsible for the shared part about decoration of user request with authentication scheme. The
 * service defines its own authentication scheme, and it could evaluate a request mutation. The aim is to have as
 * small implementation as possible. Therefore, the implemntation itself should construct the request to ZAAS with
 * a minimal requirements and process the result. The rest (common values for ZAAS, retrying, HA evaluation and
 * sanitation of user request should be done by this class).
 *
 * To prepare a new implementation of authentication scheme decoration is required to implement those methods:
 * - {@link AbstractAuthSchemeFactory#getResponseClass()} - define class of the response body (see T)
 * - {@link AbstractAuthSchemeFactory#getResponseFor401()} - construct empty response body for 401 response
 * - {@link AbstractAuthSchemeFactory#createRequest(AbstractConfig, ServerHttpRequest.Builder, ServiceInstance, Object)}
 *   - create the base part of request to the ZAAS. It requires only related request properties to the releated scheme
 * - {@link AbstractAuthSchemeFactory#processResponse(ServerWebExchange, GatewayFilterChain, Object)}
 *   - it is responsible for reading the response from the ZAAS and modifying the clients request to provide new credentials
 *
 * Example:
 *  class MyScheme extends AbstractAuthSchemeFactory<MyScheme.Config, MyResponse, MyData> {
 *
 *     @Override
 *     public GatewayFilter apply(Config config) {
 *         try {
 *             return createGatewayFilter(config, <construct common data or null>);
 *         } catch (Exception e) {
 *             return ((exchange, chain) -> {
 *                 ServerHttpRequest request = updateHeadersForError(exchange, e.getMessage());
 *                 return chain.filter(exchange.mutate().request(request).build());
 *             });
 *         }
 *     }
 *
 *     @Override
 *     protected Class<MyResponse> getResponseClass() {
 *         return MyResponse.class;
 *     }
 *
 *     @Override
 *     protected MyResponse getResponseFor401() {
 *         return new MyResponse();
 *     }
 *
 *     @Override
 *     protected WebClient.RequestHeadersSpec<?> createRequest(ServiceInstance instance, Object data) {
 *         String url = String.format("%s://%s:%d/%s/zaas/myScheme", instance.getScheme(), instance.getHost(), instance.getPort(), instance.getServiceId().toLowerCase());
 *         return webClient.post()
 *             .uri(url);
 *     }
 *
 *     @Override
 *     protected Mono<Void> processResponse(ServerWebExchange exchange, GatewayFilterChain chain, MyResponse response) {
 *         ServerHttpRequest request;
 *         if (response.getToken() != null) {
 *             request = exchange.getRequest().mutate().headers(headers ->
 *                 headers.add("mySchemeHeader", response.getToken())
 *             ).build();
 *         } else {
 *             request = updateHeadersForError(exchange, "Invalid or missing authentication.");
 *         }
 *
 *         exchange = exchange.mutate().request(request).build();
 *         return chain.filter(exchange);
 *     }
 *
 *     @EqualsAndHashCode(callSuper = true)
 *     public static class Config extends AbstractAuthSchemeFactory.AbstractConfig {
 *
 *     }
 *
 *  }
 *
 *  @Data
 *  class MyResponse {
 *
 *      private String token;
 *
 *  }
 *
 * @param <T> Class of config class. It should extend {@link AbstractAuthSchemeFactory.AbstractConfig}
 * @param <R> Class of expended response from the ZAAS
 * @param <D> Type of data object that could be constructed before any request, and it is request for creating a request
 */
public abstract class AbstractAuthSchemeFactory<T extends AbstractAuthSchemeFactory.AbstractConfig, R, D> extends AbstractGatewayFilterFactory<T> {

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
        StringUtils.equalsIgnoreCase(headerName, CLIENT_CERT_HEADER) ||
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

    /**
     * @return class of response body from ZAAS
     */
    protected abstract Class<R> getResponseClass();

    /**
     * @return empty object that is returned in the case of 401 response from ZAAS
     */
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

    /**
     * This method should construct basic request to the ZAAS (related to the authentication scheme). It should define
     * URL, body and specific headers / cookies (if they are needed). The rest of values are set by
     * {@link AbstractAuthSchemeFactory}
     *
     * @param instance - instance of the ZAAS instance that will be invoked
     * @param data - data object set in the call of {@link AbstractAuthSchemeFactory#createGatewayFilter(AbstractConfig, Object)}
     * @return builder of the request
     */
    @SuppressWarnings({
        "squid:S1452",  // the internal API cannot define generic more specificly
        "squid:S2092"   // the cookie is used just for internal purposes (off the browser)
    })
    protected abstract WebClient.RequestHeadersSpec<?> createRequest(ServiceInstance instance, D data);

    /**
     * The method responsible for reading a response from a ZAAS component and decorating of user request (ie. set
     * credentials as header, etc.)
     * @param clientCallBuilder builder of customer request (to set new credentials)
     * @param chain chain of filter to be evaluated. Method should return `return chain.filter(exchange)`
     * @param response response body from the ZAAS containing new credentials or and empty object - see {@link AbstractAuthSchemeFactory#getResponseFor401()}
     * @return response of chain evaluation (`return chain.filter(exchange)`)
     */
    @SuppressWarnings("squid:S2092")    // the cookie is used just for internal purposes (off the browser)
    protected abstract Mono<Void> processResponse(ServerWebExchange clientCallBuilder, GatewayFilterChain chain, R response);

    @SuppressWarnings("squid:S1452")    // the internal API cannot define generic more specifically
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

    @Data
    protected abstract static class AbstractConfig {

        // service ID of the target service
        private String serviceId;

    }

}
