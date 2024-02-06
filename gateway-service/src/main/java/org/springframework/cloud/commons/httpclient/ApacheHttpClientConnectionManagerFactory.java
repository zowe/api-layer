/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.springframework.cloud.commons.httpclient;

import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;

import java.util.concurrent.TimeUnit;

/**
 * Interface for creating an {@link HttpClientConnectionManager}.
 *
 * @author Ryan Baxter
 */
public interface ApacheHttpClientConnectionManagerFactory {

    /**
     * Scheme for HTTP based communication.
     */
    String HTTP_SCHEME = "http";

    /**
     * Scheme for HTTPS based communication.
     */
    String HTTPS_SCHEME = "https";

    /**
     * Creates a new {@link HttpClientConnectionManager}.
     * @param disableSslValidation If true, SSL validation will be disabled.
     * @param maxTotalConnections The total number of connections.
     * @param maxConnectionsPerRoute The total number of connections per route.
     * @param timeToLive The time a connection is allowed to exist.
     * @param timeUnit The time unit for the time-to-live value.
     * @param registryBuilder The {@link RegistryBuilder} to use in the connection
     * manager.
     * @return A new {@link HttpClientConnectionManager}.
     */
    HttpClientConnectionManager newConnectionManager(boolean disableSslValidation,
            int maxTotalConnections, int maxConnectionsPerRoute, long timeToLive,
            TimeUnit timeUnit, RegistryBuilder registryBuilder);

}
