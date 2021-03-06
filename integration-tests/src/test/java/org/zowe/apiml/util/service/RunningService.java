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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Map;

import static org.awaitility.Awaitility.await;

@Slf4j
public class RunningService {
    private final DiscoveryRequests discovery = new DiscoveryRequests();
    private Process newCachingProcess;

    private final String jarFile;
    private final String id;

    private final Map<String, String> parametersBefore;
    private final Map<String, String> parametersAfter;

    public RunningService(String id, String jarFile, Map<String, String> parametersBefore, Map<String, String> parametersAfter) {
        this.id = id;
        this.jarFile = jarFile;
        this.parametersBefore = parametersBefore;
        this.parametersAfter = parametersAfter;
    }

    public void start() throws IOException {
        log.info("Starting new Service with JAR file {} and ID {}", jarFile, id);
        stop();

        ArrayList<String> discoveryCommand = new ArrayList<>();
        discoveryCommand.add("java");
        parametersBefore
            .forEach((key1, value1) -> discoveryCommand.add(key1 + '=' + value1));

        discoveryCommand.add("-jar");
        discoveryCommand.add(jarFile);

        parametersAfter
            .forEach((key, value) -> discoveryCommand.add(key + '=' + value));

        ProcessBuilder builder1 = new ProcessBuilder(discoveryCommand);
        builder1.directory(new File("../"));
        newCachingProcess = builder1.inheritIO().start();
    }

    public void waitUntilReady() {
        await()
            .atMost(Duration.TWO_MINUTES)
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

    public void stop() {
        if (newCachingProcess != null) {
            log.info("Stopping new Service with ID {} on the port {}", id);
            newCachingProcess.destroy();
        }
    }
}
