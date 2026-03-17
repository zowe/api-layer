/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.filter;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.zowe.apiml.product.opentelemetry.OtelRequestContext;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Function;

@Component
@ConditionalOnProperty(value = "otel.sdk.disabled", havingValue = "false", matchIfMissing = true)
public class OtelRequestFilter implements WebFilter, GlobalFilter, Ordered {

    private static final String SERVICE_DISCOVERY = "discovery";
    private static final String SERVICE_API_CATALOG = "apicatalog";
    private static final String SERVICE_GATEWAY = "gateway";
    private static final String SERVICE_CACHING = "cachingservice";

    private static final Map<String, String> BASE_PATH_TO_SERVICE_ID = new HashMap<>();
    private static final int MAX_LENGTH_PATTERN;
    static {
        // homepage
        BASE_PATH_TO_SERVICE_ID.put("", SERVICE_GATEWAY);
        // determinate based on port
        //BASE_PATH_TO_SERVICE_ID.put("eureka", SERVICE_DISCOVERY);
        BASE_PATH_TO_SERVICE_ID.put("apicatalog", SERVICE_API_CATALOG);
        BASE_PATH_TO_SERVICE_ID.put("gateway", SERVICE_GATEWAY);
        BASE_PATH_TO_SERVICE_ID.put("application", SERVICE_GATEWAY);
        BASE_PATH_TO_SERVICE_ID.put("images", SERVICE_GATEWAY);
        BASE_PATH_TO_SERVICE_ID.put("cachingservice", SERVICE_CACHING);
        // swagger https://localhost:10010/v3/api-docs/<serviceId>
        BASE_PATH_TO_SERVICE_ID.put("v3/api-docs", SERVICE_GATEWAY);
        BASE_PATH_TO_SERVICE_ID.put("v3/api-docs/gateway", SERVICE_GATEWAY);
        BASE_PATH_TO_SERVICE_ID.put("v3/api-docs/apicatalog", SERVICE_API_CATALOG);
        BASE_PATH_TO_SERVICE_ID.put("v3/api-docs/discovery", SERVICE_DISCOVERY);
        BASE_PATH_TO_SERVICE_ID.put("v3/api-docs/caching", SERVICE_CACHING);

        // detect the most complicate pattern
        MAX_LENGTH_PATTERN = BASE_PATH_TO_SERVICE_ID.keySet().stream().map(k -> k.split("/").length).max(Integer::compareTo).get();
    }

    @Value("${apiml.service.hostname:localhost}")
    private String hostname;

    @Value("${apiml.internal-discovery.port:10011}")
    private int discoveryPort;

    @Value("${server.attlsServer.enabled:false}")
    private boolean isServerAttlsEnabled;

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }

    private String getServiceIdByBasePath(List<PathContainer.Element> paths) {
        // first parts of path to verify
        List<String> toCheck = new ArrayList<>(MAX_LENGTH_PATTERN);

        // only odd parts matter, the rest are slashes
        for (int i = 1, j = 1; i <= MAX_LENGTH_PATTERN && j < paths.size(); i++, j += 2) {
            toCheck.add(paths.get(j).value());
        }

        if (toCheck.isEmpty()) {
            // homepage
            return BASE_PATH_TO_SERVICE_ID.get("");
        }

        // try all combination from the longest one
        for (int i = toCheck.size(); i > 0; i--) {
            String basePath = StringUtils.join(toCheck.subList(0, i), '/');
            String serviceId = BASE_PATH_TO_SERVICE_ID.get(basePath);
            if (serviceId != null) {
                return serviceId;
            }
        }

        // cannot determinate any service, it might be set then during the routing
        return null;
    }

    void setDefaults(ServerWebExchange exchange, OtelRequestContext otelContext) {
        // detect port and serviceId by request
        String serviceId;
        int port = exchange.getRequest().getLocalAddress().getPort();
        if (port == discoveryPort) {
            serviceId = SERVICE_DISCOVERY;
        } else {
            serviceId = getServiceIdByBasePath(exchange.getRequest().getPath().elements());
        }

        // set serviceId and instanceId
        Optional.ofNullable(serviceId).ifPresent(id -> otelContext
            .serviceId(id)
            .instanceId(String.format("%s:%s:%d", hostname, id, port))
        );

        // set base information about the request
        otelContext
            .method(exchange.getRequest().getMethod())
            .scheme(isServerAttlsEnabled ? "https" : exchange.getRequest().getURI().getScheme())
            .path(exchange.getRequest().getURI().getPath());
    }

    private Mono<Void> filterInternal(ServerWebExchange exchange, Function<ServerWebExchange, Mono<Void>> filter) {
        var otelContext = OtelRequestContext.of(exchange);
        if (!otelContext.mark()) {
            // not the first call because of multiple interfaces and different type of endpoint. It is ignored
            return filter.apply(exchange);
        }

        // define default values from request perspective. they could be overwritten then
        setDefaults(exchange, otelContext);

        return filter.apply(exchange)
            // in all cases (success / error) issue the log message
            .doFinally(signalType -> otelContext.issue())
            // update response codes
            .then(Mono.fromRunnable(() -> Optional.ofNullable(exchange.getResponse())
                .map(ServerHttpResponse::getStatusCode)
                .map(HttpStatusCode::value)
                .ifPresent(otelContext::responseCode)
            ));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return filterInternal(exchange, chain::filter);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return filterInternal(exchange, chain::filter);
    }

}
