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
import org.zowe.apiml.util.config.ConfigReader;

import java.io.*;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.awaitility.Awaitility.await;

@Slf4j
public class RunningService {
    private final DiscoveryRequests discovery = new DiscoveryRequests();
    private Process newCachingProcess;

    private final String jarFile;
    private final String id;
    private String subprocessPid;
    private Map<String,String> instanceEnv = ConfigReader.environmentConfiguration().getInstanceEnv();
    private Map<String,String> instanceEnvAttls = ConfigReader.environmentConfiguration().getInstanceEnvAttls();

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

        ArrayList<String> shellCommand = new ArrayList<>();
        shellCommand.add("java");
        parametersBefore
            .forEach((key1, value1) -> shellCommand.add(key1 + '=' + value1));

        shellCommand.add("-jar");
        shellCommand.add(jarFile);

        parametersAfter
            .forEach((key, value) -> shellCommand.add(key + '=' + value));

        ProcessBuilder builder1 = new ProcessBuilder(shellCommand);
        builder1.directory(new File("../"));
        newCachingProcess = builder1.inheritIO().start();
    }

    public void startWithScript(String bashScript) {
        log.info("Starting new Service with JAR file {} and ID {}", jarFile, id);

        ProcessBuilder builder1 = new ProcessBuilder(bashScript);
        Map<String, String> envVariables = builder1.environment();
        envVariables.putAll(instanceEnvAttls);
        envVariables.put("LAUNCH_COMPONENT", jarFile);

        builder1.directory(new File("../"));
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        executorService.submit(() -> executeCommand(builder1));
    }

    private void executeCommand(ProcessBuilder pb) {
        try {
            Process terminalCommandProcess = pb.start();

            InputStream inputStream = terminalCommandProcess.getInputStream();
            BufferedReader br = new BufferedReader(
                new InputStreamReader(inputStream));
            String line;
            while ((line = br.readLine()) != null) {
                log.info(line);
                if (line.startsWith("pid")) {
                    this.subprocessPid = line.substring(line.indexOf("=") + 1);
                    log.info("found " + this.subprocessPid);
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
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
        if (subprocessPid != null) {
            ProcessBuilder pb = new ProcessBuilder("kill", "-9", subprocessPid);
            try {
                pb.inheritIO().start();
                return;
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
        if (newCachingProcess != null) {
            log.info("Stopping new Service with ID {}", id);
            newCachingProcess.destroy();
        }
    }
}
