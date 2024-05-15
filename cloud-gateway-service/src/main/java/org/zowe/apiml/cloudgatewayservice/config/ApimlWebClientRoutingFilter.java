/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import static org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter.filterRequest;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.CLIENT_RESPONSE_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.PRESERVE_HOST_HEADER_ATTRIBUTE;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.isAlreadyRouted;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.setAlreadyRouted;
import static org.zowe.apiml.constants.ApimlConstants.HTTP_CLIENT_USE_CLIENT_CERTIFICATE;

/**
 * @author Spencer Gibb
 */
public class ApimlWebClientRoutingFilter implements GlobalFilter, Ordered {

    private final WebClient webClient;
    private final WebClient webClientClientCert;

    private final ObjectProvider<List<HttpHeadersFilter>> headersFiltersProvider;

    // do not use this headersFilters directly, use getHeadersFilters() instead.
    private volatile List<HttpHeadersFilter> headersFilters;

    public ApimlWebClientRoutingFilter(WebClient webClient, WebClient webClientClientCert,
                                       ObjectProvider<List<HttpHeadersFilter>> headersFiltersProvider) {
        this.webClient = webClient;
        this.webClientClientCert = webClientClientCert;
        this.headersFiltersProvider = headersFiltersProvider;
    }

    public List<HttpHeadersFilter> getHeadersFilters() {
        if (headersFilters == null) {
            headersFilters = headersFiltersProvider.getIfAvailable();
        }
        return headersFilters;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        URI requestUrl = exchange.getRequiredAttribute(GATEWAY_REQUEST_URL_ATTR);

        String scheme = requestUrl.getScheme();
        if (isAlreadyRouted(exchange) || (!"http".equals(scheme) && !"https".equals(scheme))) {
            return chain.filter(exchange);
        }
        setAlreadyRouted(exchange);

        ServerHttpRequest request = exchange.getRequest();

        HttpMethod method = request.getMethod();

        HttpHeaders filteredHeaders = filterRequest(getHeadersFilters(), exchange);

        boolean preserveHost = exchange.getAttributeOrDefault(PRESERVE_HOST_HEADER_ATTRIBUTE, false);
        boolean useClientCert = Optional.ofNullable((Boolean) exchange.getAttribute(HTTP_CLIENT_USE_CLIENT_CERTIFICATE)).orElse(Boolean.FALSE);
        var httpClient = useClientCert ? webClientClientCert : webClient;

        RequestBodySpec bodySpec = httpClient.method(method).uri(requestUrl).headers(httpHeaders -> {
            httpHeaders.addAll(filteredHeaders);
            if (!preserveHost) {
                httpHeaders.remove(HttpHeaders.HOST);
            }
        });

        RequestHeadersSpec<?> headersSpec;
        if (requiresBody(method)) {
            headersSpec = bodySpec.body(BodyInserters.fromDataBuffers(request.getBody()));
        } else {
            headersSpec = bodySpec;
        }

        return headersSpec.exchangeToMono(res -> {
            ServerHttpResponse response = exchange.getResponse();
            response.getHeaders().putAll(res.headers().asHttpHeaders());
            response.setStatusCode(res.statusCode());
            // Defer committing the response until all route filters have run
            // Put client response as ServerWebExchange attribute and write
            // response later NettyWriteResponseFilter

            exchange.getAttributes().put(CLIENT_RESPONSE_ATTR, res);
            return response.writeWith(res.bodyToMono(DataBuffer.class));
        });
    }

    private boolean requiresBody(HttpMethod method) {
        return method.equals(HttpMethod.PUT) || method.equals(HttpMethod.POST) || method.equals(HttpMethod.PATCH);
    }

}
