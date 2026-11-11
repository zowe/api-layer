/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.eureka;

import com.netflix.discovery.shared.transport.jersey3.EurekaJersey3Client;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.pool.ConnPoolControl;
import org.apache.hc.core5.util.TimeValue;
import org.glassfish.jersey.apache5.connector.Apache5ClientProperties;
import org.glassfish.jersey.apache5.connector.Apache5ConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Replacement of {@code EurekaJersey3ClientImpl}, which is hard-wired to the Apache HttpClient 4 connector.
 * This implementation uses the Jersey Apache HttpClient 5 connector and periodically closes idle connections.
 */
@Slf4j
public class Apache5EurekaJersey3Client implements EurekaJersey3Client {

    private static final long CONNECTION_CLEANER_INTERVAL_MS = 30_000;

    private final Client jerseyClient;
    private final ScheduledExecutorService connectionCleaner;
    private final Runnable connectionCleanerTask;

    public Apache5EurekaJersey3Client(int connectionTimeout, int readTimeout, int connectionIdleTimeout, ClientConfig clientConfig) {
        try {
            // JSON (de)serialization is provided by DiscoveryJerseyProvider registered in the config
            clientConfig.property(ClientProperties.JSON_PROCESSING_FEATURE_DISABLE, Boolean.TRUE);
            clientConfig.property(ClientProperties.MOXY_JSON_FEATURE_DISABLE, Boolean.TRUE);
            clientConfig.connectorProvider(new Apache5ConnectorProvider());
            clientConfig.property(ClientProperties.CONNECT_TIMEOUT, connectionTimeout);
            clientConfig.property(ClientProperties.READ_TIMEOUT, readTimeout);
            jerseyClient = ClientBuilder.newClient(clientConfig);

            connectionCleanerTask = () -> closeIdleConnections(connectionIdleTimeout);
            connectionCleaner = Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "Apiml-Eureka-Replication-Connection-Cleaner");
                thread.setDaemon(true);
                return thread;
            });
            connectionCleaner.scheduleWithFixedDelay(connectionCleanerTask,
                CONNECTION_CLEANER_INTERVAL_MS, CONNECTION_CLEANER_INTERVAL_MS, TimeUnit.MILLISECONDS);
        } catch (Throwable e) {
            throw new RuntimeException("Cannot create Jersey3 client", e);
        }
    }

    private void closeIdleConnections(int connectionIdleTimeout) {
        try {
            var cm = (ConnPoolControl<?>) jerseyClient.getConfiguration()
                .getProperty(Apache5ClientProperties.CONNECTION_MANAGER);
            cm.closeIdle(TimeValue.of(connectionIdleTimeout, TimeUnit.SECONDS));
        } catch (Exception e) {
            log.error("Cannot clean connections", e);
        }
    }

    @Override
    public Client getClient() {
        return jerseyClient;
    }

    @Override
    public void destroyResources() {
        // execute the cleaner one final time during shutdown
        connectionCleaner.execute(connectionCleanerTask);
        connectionCleaner.shutdown();
        jerseyClient.close();
    }

}
