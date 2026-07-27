/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.ws;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.zowe.apiml.gateway.filters.pre.SecFetchSitePolicy;

import java.util.Map;

/**
 * Applies {@link SecFetchSitePolicy} to the WebSocket proxy handshake. The gateway's WebSocket
 * proxying is registered as a Spring {@code WebSocketHandler} (see {@link GatewayWebSocketConfigurer}),
 * a dispatch path entirely separate from the Zuul pre/route/post filter pipeline, so
 * {@code SecFetchSiteFilter} never sees these requests - this interceptor is what applies the same
 * Fetch Metadata check to the handshake instead.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecFetchSiteHandshakeInterceptor implements HandshakeInterceptor {

    private final SecFetchSitePolicy secFetchSitePolicy;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (secFetchSitePolicy.isAllowed(request.getHeaders()::getFirst)) {
            return true;
        }

        log.debug("Blocked cross-site WebSocket handshake {} {} - Sec-Fetch-Site={}, CORS is not enabled",
            request.getMethod(), request.getURI(), request.getHeaders().getFirst(SecFetchSitePolicy.SEC_FETCH_SITE_HEADER));
        response.setStatusCode(HttpStatus.FORBIDDEN);
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }

}
