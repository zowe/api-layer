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

import org.apache.http.HttpHost;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectionRequest;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApimlPoolingHttpClientConnectionManagerTest {
    private HttpRoute route;
    private Object state;

    private ApimlPoolingHttpClientConnectionManager connectionManager;

    @BeforeEach
    public void setUp() {
        route = new HttpRoute(new HttpHost("localhost", 8000));
        state = new Object();

        RegistryBuilder<ConnectionSocketFactory> socketFactoryRegistryBuilder = RegistryBuilder
            .<ConnectionSocketFactory>create().register("http", PlainConnectionSocketFactory.getSocketFactory());
        connectionManager = new ApimlPoolingHttpClientConnectionManager(socketFactoryRegistryBuilder.build(),10_000);
    }

    @Test
    void whenRequestConnection_thenReturnConnectionRequest() {
        ConnectionRequest connectionRequest = connectionManager.requestConnection(route, state);
        assertNotNull(connectionRequest);
    }

    @Test
    void givenNoSocketRegistry_whenCreateConnection_thenThrowError() {
        assertThrows(IllegalArgumentException.class, () -> new ApimlPoolingHttpClientConnectionManager(null,10_000));
    }
}
