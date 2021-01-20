/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.cachingservice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Duration;
import org.zowe.apiml.util.DiscoveryRequests;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import static org.awaitility.Awaitility.await;

@Slf4j
@RequiredArgsConstructor
public class CachingService {
    private final String id;
    private final String evictionStrategy;
    private final String port;

    public CachingService() {
        id = "cachingoldest";
        evictionStrategy = "removeOldest";
        port = "10023";
    }

    private DiscoveryRequests discovery = new DiscoveryRequests();
    private Process newCachingProcess;

    public void start() throws IOException {
        log.info("Starting new Caching Service with ID {} on the port {}", id, port);
        stop();

        ArrayList<String> discoveryCommand = new ArrayList<>();
        discoveryCommand.add("java");
        discoveryCommand.add("-Dcaching.storage.evictionStrategy=" + evictionStrategy);
        discoveryCommand.add("-Dapiml.service.serviceId=" + id);
        discoveryCommand.add("-Dapiml.service.port=" + port);
        discoveryCommand.add("-jar");
        discoveryCommand.add("./caching-service/build/libs/caching-service.jar");

        ProcessBuilder builder1 = new ProcessBuilder(discoveryCommand);
        builder1.directory(new File("../"));
        newCachingProcess = builder1.inheritIO().start();

        await()
            .atMost(Duration.ONE_MINUTE)
        .with()
            .pollInterval(Duration.TEN_SECONDS)
            .until(this::isServiceProperlyRegistered);
    }

    public boolean isServiceProperlyRegistered() {
        try {
            if (discovery.isApplicationRegistered(id)) {
                return true;
            }
        } catch (URISyntaxException e) {
            // Ignorable exception.
            e.printStackTrace();
        }

        return false;
    }

    public URI getBaseUrl() {
        return HttpRequestUtils.getUriFromGateway("/" + id + "/api/v1/cache");
    }

    public void stop() {
        if (newCachingProcess != null) {
            log.info("Stopping new Caching Service with ID {} on the port {}", id, port);
            newCachingProcess.destroy();
        }
    }
}
