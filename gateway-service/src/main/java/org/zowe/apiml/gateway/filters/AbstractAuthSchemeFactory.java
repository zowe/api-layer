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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.SslInfo;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.gateway.service.InstanceInfoService;
import org.zowe.apiml.gateway.x509.X509Util;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.util.CookieUtil;
import reactor.core.publisher.Mono;

import java.net.HttpCookie;
import java.security.cert.CertificateEncodingException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.apache.hc.core5.http.HttpStatus.SC_OK;
import static org.apache.hc.core5.http.HttpStatus.SC_UNAUTHORIZED;
import static org.zowe.apiml.constants.ApimlConstants.PAT_COOKIE_AUTH_NAME;
import static org.zowe.apiml.constants.ApimlConstants.PAT_HEADER_NAME;
import static org.zowe.apiml.gateway.x509.ForwardClientCertFilterFactory.CLIENT_CERT_HEADER;
import static org.zowe.apiml.security.SecurityUtils.COOKIE_AUTH_NAME;

/**
 * This class is responsible for the shared part about decoration of user request with authentication scheme. The
 * service defines its own authentication scheme, and it could evaluate a request mutation. The aim is to have as
 * small implementation as possible. Therefore, the implementation itself should construct the request to ZAAS with
 * a minimal requirements and process the result. The rest (common values for ZAAS, retrying, HA evaluation and
 * sanitation of user request should be done by this class).
 * <p>
 * To prepare a new implementation of authentication scheme decoration is required to implement those methods:
 * - {@link AbstractAuthSchemeFactory#getResponseClass()} - define class of the response body (see T)
 * - {@link AbstractAuthSchemeFactory#createRequest(AbstractConfig, ServerHttpRequest.Builder, ServiceInstance, Object, ServerHttpRequest request)}
 * - create the base part of request to the ZAAS. It requires only related request properties to the related scheme
 * - {@link AbstractAuthSchemeFactory#processResponse(ServerWebExchange, GatewayFilterChain, AuthorizationResponse<R>)}
 * - it is responsible for reading the response from the ZAAS and modifying the clients request to provide new credentials
 * <p>
 * Example:
 * class MyScheme extends AbstractAuthSchemeFactory<MyScheme.Config, MyResponse, MyData> {
 *
 * @param <T> Class of config class. It should extend {@link AbstractAuthSchemeFactory.AbstractConfig}
 * @param <R> Class of expended response from the ZAAS
 * @param <D> Type of data object that could be constructed before any request, and it is request for creating a request
 * @Override public GatewayFilter apply(Config config) {
 * try {
 * return createGatewayFilter(config, <construct common data or null>);
 * } catch (Exception e) {
 * return ((exchange, chain) -> {
 * ServerHttpRequest request = updateHeadersForError(exchange, e.getMessage());
 * return chain.filter(exchange.mutate().request(request).build());
 * });
 * }
 * }
 * @Override protected Class<MyResponse> getResponseClass() {
 * return MyResponse.class;
 * }
 * @Override protected MyResponse getResponseFor401() {
 * return new MyResponse();
 * }
 * @Override protected WebClient.RequestHeadersSpec<?> createRequest(ServiceInstance instance, Object data) {
 * String url = String.format("%s://%s:%d/%s/zaas/myScheme", instance.getScheme(), instance.getHost(), instance.getPort(), instance.getServiceId().toLowerCase());
 * return webClient.post().uri(url);
 * }
 * @Override protected Mono<Void> processResponse(ServerWebExchange exchange, GatewayFilterChain chain, MyResponse response) {
 * ServerHttpRequest request;
 * if (response.getToken() != null) {
 * request = exchange.getRequest().mutate().headers(headers ->
 * headers.add("mySchemeHeader", response.getToken())
 * ).build();
 * } else {
 * request = updateHeadersForError(exchange, "Invalid or missing authentication");
 * }
 * exchange = exchange.mutate().request(request).build();
 * return chain.filter(exchange);
 * }
 * @EqualsAndHashCode(callSuper = true)
 * public static class Config extends AbstractAuthSchemeFactory.AbstractConfig {
 * }
 * }
 * @Data class MyResponse {
 * private String token;
 * }
 */
public abstract class AbstractAuthSchemeFactory<T extends AbstractAuthSchemeFactory.AbstractConfig, R, D> extends AbstractGatewayFilterFactory<T> {

    private static final String HEADER_SERVICE_ID = "X-Service-Id";

    private static final String[] CERTIFICATE_HEADERS = {
        "X-Certificate-Public",
        "X-Certificate-DistinguishedName",
        "X-Certificate-CommonName"
    };

    private static final Predicate<String> CERTIFICATE_HEADERS_TEST = headerName ->
        StringUtils.equalsIgnoreCase(headerName, CERTIFICATE_HEADERS[0]) ||
            StringUtils.equalsIgnoreCase(headerName, CERTIFICATE_HEADERS[1]) ||
            StringUtils.equalsIgnoreCase(headerName, CERTIFICATE_HEADERS[2]);

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
            CERTIFICATE_HEADERS_TEST.test(headerName) ||
            StringUtils.equalsIgnoreCase(headerName, "X-SAF-Token") ||
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

    private Mono<List<ServiceInstance>> getZaasInstances() {
        return instanceInfoService.getServiceInstance("zaas");
    }

    private Mono<AuthorizationResponse<R>> requestWithHa(
        Iterator<ServiceInstance> serviceInstanceIterator,
        Function<ServiceInstance, WebClient.RequestHeadersSpec<?>> requestCreator
    ) {
        return requestCreator.apply(serviceInstanceIterator.next())
            .exchangeToMono(clientResp -> switch (clientResp.statusCode().value()) {
                case SC_UNAUTHORIZED -> Mono.just(new AuthorizationResponse<R>(clientResp.headers(), null));
                case SC_OK ->
                    clientResp.bodyToMono(getResponseClass()).map(b -> new AuthorizationResponse<R>(clientResp.headers(), b));
                default -> Mono.empty();
            })
            .switchIfEmpty(serviceInstanceIterator.hasNext() ?
                requestWithHa(serviceInstanceIterator, requestCreator) : Mono.empty()
            );
    }

    protected Mono<Void> invoke(
        List<ServiceInstance> serviceInstances,
        Function<ServiceInstance, WebClient.RequestHeadersSpec<?>> requestCreator,
        Function<? super AuthorizationResponse<R>, ? extends Mono<Void>> responseProcessor
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
     * @param data     - data object set in the call of {@link AbstractAuthSchemeFactory#createGatewayFilter(AbstractConfig, Object)}
     * @return builder of the request
     */
    @SuppressWarnings({
        "squid:S1452",  // the internal API cannot define generic more specifically
        "squid:S2092"   // the cookie is used just for internal purposes (off the browser)
    })
    protected abstract WebClient.RequestHeadersSpec<?> createRequest(ServiceInstance instance, D data);

    /**
     * The method responsible for reading a response from a ZAAS component and decorating of user request (i.e. set
     * credentials as header, etc.)
     *
     * @param clientCallBuilder builder of customer request (to set new credentials)
     * @param chain             chain of filter to be evaluated. Method should return `return chain.filter(exchange)`
     * @param response          response body from the ZAAS containing new credentials or and empty object
     * @return response of chain evaluation (`return chain.filter(exchange)`)
     */
    @SuppressWarnings("squid:S2092")    // the cookie is used just for internal purposes (off the browser)
    protected abstract Mono<Void> processResponse(ServerWebExchange clientCallBuilder, GatewayFilterChain chain, AuthorizationResponse<R> response);

    @SuppressWarnings("squid:S1452")    // the internal API cannot define generic more specifically
    protected WebClient.RequestHeadersSpec<?> createRequest(AbstractConfig config, ServerHttpRequest.Builder clientRequestbuilder, ServiceInstance instance, D data, ServerHttpRequest request) {
        WebClient.RequestHeadersSpec<?> zaasCallBuilder = createRequest(instance, data);

        clientRequestbuilder
            .headers(headers -> {
                // get all current cookies
                List<HttpCookie> cookies = CookieUtil.readCookies(headers).toList();

                // set in the request to ZAAS all cookies and headers that contain credentials
                headers.entrySet().stream()
                    .filter(e -> CREDENTIALS_HEADER_INPUT.test(e.getKey()))
                    .forEach(e -> zaasCallBuilder.header(e.getKey(), e.getValue().toArray(new String[0])));
                cookies.stream()
                    .filter(CREDENTIALS_COOKIE_INPUT)
                    .forEach(c -> zaasCallBuilder.cookie(c.getName(), c.getValue()));

                // add common headers to ZAAS
                zaasCallBuilder.header(HEADER_SERVICE_ID, config.serviceId);

                // add client certificate when present
                setClientCertificate(zaasCallBuilder, request.getSslInfo());
            });

        return zaasCallBuilder;
    }

    /**
     * This method remove a necessary subset of credentials in case of authentication fail. If ZAAS cannot generate a
     * new credentials (ie. because of basic authentication, expired token, etc.) the Gateway should provide the original
     * credentials passed by a user. But there are headers that could be removed to avoid misusing (see forwarding
     * certificate - user cannot provide a public certificate to take foreign privilleges).
     * It also set the header to describe an authentication error.
     *
     * @param exchange     exchange of the user request resent to a service
     * @param errorMessage message to be set in the X-Zowe-Auth-Failure header
     * @returnmutated request
     */
    protected ServerHttpRequest cleanHeadersOnAuthFail(ServerWebExchange exchange, String errorMessage) {
        return exchange.getRequest().mutate().headers(headers -> {
            // update original request - to remove all potential headers and cookies with credentials
            Arrays.stream(CERTIFICATE_HEADERS).forEach(headers::remove);

            // set error header in both side (request to the service, response to the user)
            headers.add(ApimlConstants.AUTH_FAIL_HEADER, errorMessage);
            exchange.getResponse().getHeaders().add(ApimlConstants.AUTH_FAIL_HEADER, errorMessage);
        }).build();
    }

    /**
     * This method removes from the request all headers and cookie related to the authentication. It is necessary to send
     * the request with multiple auth values. The Gateway would set a new credentials in this case
     *
     * @param exchange exchange of the user request resent to a service
     * @return mutated request
     */
    protected ServerHttpRequest cleanHeadersOnAuthSuccess(ServerWebExchange exchange) {
        return exchange.getRequest().mutate().headers(headers -> {
            // get all current cookies
            List<HttpCookie> cookies = CookieUtil.readCookies(headers).toList();

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
            ).toList();

            headers.clear();
            newHeaders.forEach(newHeader -> headers.add(newHeader.getKey(), newHeader.getValue()));
        }).build();
    }

    protected GatewayFilter createGatewayFilter(AbstractConfig config, D data) {
        return (exchange, chain) -> getZaasInstances().flatMap(
            instances -> {
                ServerHttpRequest.Builder clientCallBuilder = exchange.getRequest().mutate();
                return invoke(
                    instances,
                    instance -> createRequest(config, clientCallBuilder, instance, data, exchange.getRequest()),
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

    protected ServerHttpRequest updateHeadersForError(ServerWebExchange exchange, String errorMessage) {
        ServerHttpRequest request = addRequestHeader(exchange, ApimlConstants.AUTH_FAIL_HEADER, errorMessage);
        exchange.getResponse().getHeaders().add(ApimlConstants.AUTH_FAIL_HEADER, errorMessage);
        return request;
    }

    protected void setClientCertificate(WebClient.RequestHeadersSpec<?> callBuilder, SslInfo sslInfo) {
        try {
            String encodedCertificate = X509Util.getEncodedClientCertificate(sslInfo);
            if (encodedCertificate != null) {
                callBuilder.header(CLIENT_CERT_HEADER, encodedCertificate);
            }
        } catch (CertificateEncodingException e) {
            callBuilder.header(ApimlConstants.AUTH_FAIL_HEADER, "Invalid client certificate in request. Error message: " + e.getMessage());
        }
    }

    @Data
    protected abstract static class AbstractConfig {

        // service ID of the target service
        private String serviceId;

    }

    @AllArgsConstructor
    @Getter
    public static class AuthorizationResponse<R> {

        private ClientResponse.Headers headers;
        private R body;

    }

}
