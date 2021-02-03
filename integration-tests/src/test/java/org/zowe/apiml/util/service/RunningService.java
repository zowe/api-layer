/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.util.service;

import lombok.extern.slf4j.Slf4j;
import org.awaitility.Duration;
import org.zowe.apiml.util.DiscoveryRequests;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.awaitility.Awaitility.await;

@Slf4j
public class RunningService {
    private DiscoveryRequests discovery = new DiscoveryRequests();
    private Process newCachingProcess;

    private String id;
    private int port;

    private Map<String, String> parameters = new HashMap<>();

    public RunningService() {
        id = "test-service";
        port = 10562;
    }

    public RunningService(String id, int port, Map<String, String> parameters) {
        this.id = id;
        this.port = port;
    }

    public void start() throws IOException {
        log.info("Starting new Service with ID {} on the port {}", id, port);
        stop();

        ArrayList<String> discoveryCommand = new ArrayList<>();
        discoveryCommand.add("java");
        discoveryCommand.add("-Dapiml.service.serviceId=" + id);
        discoveryCommand.add("-Dapiml.service.port=" + port);
        parameters
            .entrySet()
            .forEach(
                entry -> discoveryCommand.add(entry.getKey() + '=' + entry.getValue())
            );

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
            log.info("Stopping new Service with ID {} on the port {}", id, port);
            newCachingProcess.destroy();
        }
    }
}
