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

import org.springframework.http.HttpHeaders;
import org.springframework.security.web.server.header.ServerHttpHeadersWriter;
import org.springframework.security.web.server.header.StrictTransportSecurityServerHttpHeadersWriter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.time.Duration;

/**
 * Custom implementation of ServerHttpHeadersWriter for handling HTTP Strict Transport Security (HSTS).
 *
 * <p>This class overrides the standard HSTS header behavior to ensure header inclusion even when
 * Application-Transparent TLS (AT-TLS) is enabled. By default, the StrictTransportSecurityServerHttpHeadersWriter
 * omits the HSTS header when detecting plain HTTP mode, which occurs during AT-TLS operation.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Forces HSTS header inclusion regardless of transport security detection</li>
 *   <li>Maintains compatibility with AT-TLS enabled environments</li>
 * </ul>
 *
 * @see WebSecurity
 * @see StrictTransportSecurityServerHttpHeadersWriter
 */
public class CustomHstsServerHttpHeadersWriter implements ServerHttpHeadersWriter {

    private static final String DEFAULT_MAX_AGE = "max-age=" + Duration.ofDays(365L).getSeconds();
    private static final String DEFAULT_INCLUDE_SUBDOMAINS = "; includeSubDomains";
    private final String headerValue = DEFAULT_MAX_AGE + DEFAULT_INCLUDE_SUBDOMAINS;

    @Override
    public Mono<Void> writeHttpHeaders(ServerWebExchange exchange) {
        HttpHeaders headers = exchange.getResponse().getHeaders();
        headers.set("Strict-Transport-Security", headerValue);
        return Mono.empty();
    }
}
