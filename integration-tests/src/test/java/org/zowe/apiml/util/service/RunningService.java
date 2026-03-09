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
    private ExecutorService executorService = Executors.newFixedThreadPool(1);

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
        log.info("Starting new Service via shell command with JAR file {} and ID {}", jarFile, id);
        stop();

        ArrayList<String> shellCommand = new ArrayList<>();

        // If JAVA_HOME is defined in environment variable, use it, otherwise assume in PATH
        String path = Optional.ofNullable(System.getenv("JAVA_HOME"))
                                .map(javaHome -> javaHome + "/bin/")
                                .orElse("");

        if (envs != null && envs.length > 0) {
            path = Arrays.stream(envs).collect(joining(" ")) + "&&" + path;
        }

        shellCommand.add(path + "java");
        shellCommand.addAll(Arrays.asList(
            "--add-opens=java.base/java.lang=ALL-UNNAMED",
            "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
            "--add-opens=java.base/java.nio.channels.spi=ALL-UNNAMED",
            "--add-opens=java.base/java.util=ALL-UNNAMED",
            "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED",
            "--add-opens=java.base/javax.net.ssl=ALL-UNNAMED",
            "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
            "--add-opens=java.base/java.io=ALL-UNNAMED"
        ));
        if (parametersBefore != null) {
            parametersBefore
                .forEach((key1, value1) -> shellCommand.add(key1 + '=' + value1));
        }

        shellCommand.add("-jar");
        shellCommand.add(jarFile);

        if (parametersAfter != null) {
            parametersAfter
                .forEach((key, value) -> shellCommand.add(key + '=' + value));
        }

        try {
            ProcessBuilder builder1 = new ProcessBuilder(shellCommand);
            builder1.directory(new File("../"));
            process = builder1.inheritIO().start();
        } catch (Exception e) {
            log.error("Failed starting: " + this.id, e);
        }
    }

    public void startWithScript(String binPath, Map<String, String> env) {
        log.info("Starting new Service via start.sh script with JAR file {} and ID {}", jarFile, id);
        ProcessBuilder builder1 = new ProcessBuilder(binPath + "/start.sh");
        Map<String, String> envVariables = builder1.environment();
        envVariables.putAll(env);
        envVariables.put("LAUNCH_COMPONENT", jarFile);
        File binFolder = new File("../");
        builder1.directory(binFolder);
        executorService.submit(() -> executeCommand(builder1));
    }

    private void executeCommand(ProcessBuilder pb) {
        try {
            process = pb.start();

            InputStream inputStream = process.getInputStream();
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
        if (process == null) {
            return;
        }

        log.info("Service with ID {} is going to be stopped", id);
        String pid = subprocessPid;
        if (pid == null) {
            pid = String.valueOf(process.pid());
            log.debug("Subprocess ID was not found, the main will be used: {}", pid);
        }

        ProcessBuilder pb = new ProcessBuilder("kill", "-9", pid);
        try {
            pb.inheritIO().start().waitFor();
            log.debug("Kill command was issued");
            subprocessPid = null;
        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage());
        }

        if (process != null) {
            try {
                log.debug("Waiting for process to terminate");
                process.waitFor();
            } catch (InterruptedException e) {
                log.debug("Service {} was interrupted", id);
            }
            log.debug("Destroying process wrapper class");
            process.destroy();
            process = null;
        }

        log.debug("Stopping the executorService");
        executorService.shutdown();
        log.info("Service with ID {} was stopped", id);
    }

}
