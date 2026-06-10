/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.config;

import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.config.HttpClientProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.NettyRoutingFilter;
import org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.web.server.ServerWebExchange;
import org.zowe.apiml.security.common.error.ServiceAccessClassifier;
import org.zowe.apiml.security.common.error.ServiceNotAccessibleException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.zowe.apiml.constants.ApimlConstants.HTTP_CLIENT_USE_CLIENT_CERTIFICATE;

@Slf4j
public class NettyRoutingFilterApiml extends NettyRoutingFilter {

    private final HttpClient httpClientNoCert;
    private final HttpClient httpClientClientCert;

    @Value("${apiml.connection.timeout:60000}")
    private int requestTimeout;

    public NettyRoutingFilterApiml(
        HttpClient httpClientNoCert,
        HttpClient httpClientClientCert,
        ObjectProvider<List<HttpHeadersFilter>> headersFiltersProvider,
        HttpClientProperties properties
    ) {
        super(null, headersFiltersProvider, properties);
        this.httpClientNoCert = httpClientNoCert;
        this.httpClientClientCert = httpClientClientCert;
    }

    static Integer getInteger(Object connectTimeoutAttr) {
        int connectTimeout;
        if (connectTimeoutAttr instanceof Integer) {
            connectTimeout = (Integer) connectTimeoutAttr;
        } else {
            connectTimeout = Integer.parseInt(connectTimeoutAttr.toString());
        }
        return connectTimeout;
    }

    @Override
    protected HttpClient getHttpClient(Route route, ServerWebExchange exchange) {
        // select proper HttpClient instance by attribute apiml.useClientCert
        var useClientCert = Optional.ofNullable((Boolean) exchange.getAttribute(HTTP_CLIENT_USE_CLIENT_CERTIFICATE)).orElse(Boolean.FALSE);
        var httpClient = useClientCert ? httpClientClientCert : httpClientNoCert;

        log.debug("Using client with keystore {}", useClientCert);
        var connectTimeoutAttr = route.getMetadata().get("apiml.connectTimeout");
        var responseTimeoutAttr = route.getMetadata().get("apiml.responseTimeout");

        var responseTimeoutResult = responseTimeoutAttr != null ? Long.parseLong(String.valueOf(responseTimeoutAttr)) : requestTimeout;
        var connectTimeoutResult = connectTimeoutAttr != null ? getInteger(connectTimeoutAttr) : requestTimeout;
        httpClient = httpClient
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutResult)
            .responseTimeout(Duration.ofMillis(responseTimeoutResult));

        return httpClient;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return super.filter(exchange, chain).onErrorResume(e -> {
            if (ServiceAccessClassifier.hasSslOrCertificateCause(e)) {
                Throwable rootCause = findCause(e);
                log.debug("SSL error detected for {}: {}", exchange.getRequest().getURI(), rootCause.getMessage());
                return Mono.error(new SSLException(rootCause.getMessage(), rootCause));
            }
            if (ServiceAccessClassifier.isServiceUnavailable(e)) {
                log.debug("Connection to {} was not established: {}", exchange.getRequest().getURI(), e.getMessage());
                var uri = exchange.getRequest().getURI();
                return Mono.error(new ServiceNotAccessibleException(String.format("Service is not available at %s://%s:%d", uri.getScheme(), uri.getHost(), uri.getPort()), e));
            }
            return Mono.error(e);
        });
    }

    static boolean isServiceUnavailable(Throwable error) {
        return ServiceAccessClassifier.isServiceUnavailable(error);
    }

    /**
     * Walks the exception cause chain to find the root cause.
     *
     * @param error the exception to inspect
     * @return the deepest cause in the chain, or the original error if no cause exists
     */
    static Throwable findCause(Throwable error) {
        Throwable current = error;
        Throwable cause = current.getCause();
        while (cause != null && cause != current) {
            current = cause;
            cause = current.getCause();
        }
        return current;
    }
}
