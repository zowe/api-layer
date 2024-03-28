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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class RunningService {
    private Process process;

    private final String jarFile;
    private final String id;
    private String subprocessPid;

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
        process = builder1.inheritIO().start();
    }

    public void startWithScript(String bashScript, Map<String, String> env) {
        log.info("Starting new Service using script with JAR file {} and ID {}", jarFile, id);

        ProcessBuilder builder1 = new ProcessBuilder(bashScript);
        Map<String, String> envVariables = builder1.environment();
        envVariables.putAll(env);
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
                System.out.println(line);
            }
            int exitCode = terminalCommandProcess.waitFor();
            System.out.println("\nExited with error code : " + exitCode);
        } catch (Exception e) {
            log.error("Error during service startup {}", e.getMessage());
        }
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
        if (process != null) {
            log.info("Stopping new Service with ID {}", id);
            process.destroy();
        }
    }
}
