/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security;

import lombok.NonNull;
import org.apache.http.config.Registry;
import org.apache.http.conn.ConnectionRequest;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.pool.PoolStats;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.message.yaml.YamlMessageServiceInstance;

import java.util.concurrent.TimeUnit;

/**
 * Used for custom pooling http connection management.
 */
public class ApimlPoolingHttpClientConnectionManager extends PoolingHttpClientConnectionManager {

    private final ApimlLogger apimlLog = ApimlLogger.of(ApimlPoolingHttpClientConnectionManager.class, YamlMessageServiceInstance.getInstance());

    public ApimlPoolingHttpClientConnectionManager(@NonNull Registry<ConnectionSocketFactory> socketFactoryRegistry, int timeToLive) {
        super(socketFactoryRegistry, null, null, null, timeToLive, TimeUnit.MILLISECONDS);
    }

    /**
     * Override requestConnection to log a warning when connection limits are reached. No other behaviour is changed.
     */
    @Override
    public ConnectionRequest requestConnection(HttpRoute route, Object state) {
        int totalLimit = super.getMaxTotal();
        PoolStats totalStats = super.getTotalStats();
        int totalConnections = totalStats.getLeased();
        if (totalConnections >= totalLimit) {
            apimlLog.log("org.zowe.apiml.common.totalConnectionLimitReached", totalLimit);
        }

        // Check route connection limit even if hit total limit as both limits could be reached

        int routeLimit = super.getMaxPerRoute(route);
        PoolStats routeStats = super.getStats(route);
        int routeConnections = routeStats.getLeased();
        if (routeConnections >= routeLimit) {
            apimlLog.log("org.zowe.apiml.common.gatewayRouteConnectionLimitReached", routeLimit, route.toString());
        }

        return super.requestConnection(route, state);
    }

}
