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

import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.io.LeaseRequest;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ApimlPoolingHttpClientConnectionManagerTest {
    private HttpRoute route;
    private Object state;

    private ApimlPoolingHttpClientConnectionManager connectionManager;

    @BeforeEach
    public void setUp() {
        route = new HttpRoute(new HttpHost("localhost", 8000));
        state = new Object();

        var socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create().register("http", PlainConnectionSocketFactory.getSocketFactory());
        connectionManager = new ApimlPoolingHttpClientConnectionManager(socketFactoryRegistry.build(), 10_000);
    }

    @Test
    void whenRequestConnection_thenReturnConnectionRequest() {
        LeaseRequest leaseRequest = connectionManager.lease("id", route, state);
        assertNotNull(leaseRequest);
    }

}
