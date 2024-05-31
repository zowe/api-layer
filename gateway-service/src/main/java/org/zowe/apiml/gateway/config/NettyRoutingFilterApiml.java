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
import io.netty.handler.ssl.SslContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.config.HttpClientProperties;
import org.springframework.cloud.gateway.filter.NettyRoutingFilter;
import org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.web.server.ServerWebExchange;
import reactor.netty.http.client.HttpClient;

import java.util.List;
import java.util.Optional;

import static org.springframework.cloud.gateway.support.RouteMetadataUtils.CONNECT_TIMEOUT_ATTR;
import static org.zowe.apiml.constants.ApimlConstants.HTTP_CLIENT_USE_CLIENT_CERTIFICATE;

@Slf4j
public class NettyRoutingFilterApiml extends NettyRoutingFilter {

    private final HttpClient httpClientNoCert;
    private final HttpClient httpClientClientCert;

    public NettyRoutingFilterApiml(
        HttpClient httpClient,
        ObjectProvider<List<HttpHeadersFilter>> headersFiltersProvider,
        HttpClientProperties properties,
        SslContext justTruststore,
        SslContext withKeystore
    ) {
        super(null, headersFiltersProvider, properties);

        // construct http clients with different SSL configuration - with / without client certs
        httpClientNoCert = httpClient.secure(sslContextSpec -> sslContextSpec.sslContext(justTruststore));
        httpClientClientCert = httpClient.secure(sslContextSpec -> sslContextSpec.sslContext(withKeystore));
    }

    static Integer getInteger(Object connectTimeoutAttr) {
        Integer connectTimeout;
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
        boolean useClientCert = Optional.ofNullable((Boolean) exchange.getAttribute(HTTP_CLIENT_USE_CLIENT_CERTIFICATE)).orElse(Boolean.FALSE);
        HttpClient httpClient = useClientCert ? httpClientClientCert : httpClientNoCert;

        log.debug("Using client with keystore {}", useClientCert);
        Object connectTimeoutAttr = route.getMetadata().get(CONNECT_TIMEOUT_ATTR);
        if (connectTimeoutAttr != null) {
            // if there is configured timeout, respect it
            Integer connectTimeout = getInteger(connectTimeoutAttr);
            return httpClient.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout);
        }

        // otherwise just return selected HttpClient
        return httpClient;
    }

}
