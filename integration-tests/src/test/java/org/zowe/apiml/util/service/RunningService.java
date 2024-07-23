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
import org.junit.platform.commons.util.StringUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.stream.Collectors.joining;

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

    public void start(String... envs) throws IOException {
        log.info("Starting new Service with JAR file {} and ID {}", jarFile, id);
        stop();

        ArrayList<String> shellCommand = new ArrayList<>();

        // If JAVA_HOME is defined in environment variable, use it, otherwise assume in PATH
        String path = Optional.ofNullable(System.getenv("JAVA_HOME"))
                                .map(javaHome -> javaHome + "/bin/")
                                .orElse("");

        if (envs != null) {
            path = Arrays.stream(envs).collect(joining(" ")) + "&&" + path;
        }

        shellCommand.add(path + "java");
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

    public void startWithScript(String binPath, Map<String, String> env) {
        log.info("Starting new Service with JAR file {} and ID {}", jarFile, id);
        ProcessBuilder builder1 = new ProcessBuilder(binPath + "/start.sh");
        Map<String, String> envVariables = builder1.environment();
        envVariables.putAll(env);
        envVariables.put("LAUNCH_COMPONENT", jarFile);
        File binFolder = new File("../");
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        builder1.directory(binFolder);
        executorService.submit(() -> executeCommand(builder1));
    }

    private void executeCommand(ProcessBuilder pb) {
        try {
            Process terminalCommandProcess = pb.start();

            InputStream inputStream = terminalCommandProcess.getInputStream();
            BufferedReader br = new BufferedReader(
                new InputStreamReader(inputStream));
            String line;
            while (StringUtils.isBlank(this.subprocessPid) && (line = br.readLine()) != null) {
                log.info(line);
                if (line.startsWith("pid")) {
                    this.subprocessPid = line.substring(line.indexOf("=") + 1);
                    log.info("found PID:" + this.subprocessPid + " for service: {}", id);
                }
            }

            while ((line = br.readLine()) != null) {
                log.info(line);
            }
        } catch (IOException e) {
            log.error(e.getMessage());
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
