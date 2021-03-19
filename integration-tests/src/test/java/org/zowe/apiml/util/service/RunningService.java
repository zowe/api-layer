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
        envVariables.put("LAUNCH_COMPONENT", jarFile);
        envVariables.put("CMMN_LB", "build/libs/api-layer-lite-lib-all.jar");
        envVariables.put("ZOWE_EXPLORER_HOST", "localhost");
        envVariables.put("ZOWE_PREFIX", "ZWE");
        envVariables.put("CATALOG_PORT", "10014");
        envVariables.put("DISCOVERY_PORT", "10011");
        envVariables.put("GATEWAY_PORT", "10010");
        envVariables.put("APIML_ALLOW_ENCODED_SLASHES", "true");
        envVariables.put("APIML_PREFER_IP_ADDRESS", "false");
        envVariables.put("APIML_GATEWAY_TIMEOUT_MILLIS", "10000");
        envVariables.put("APIML_SECURITY_X509_ENABLED", "true");
        envVariables.put("APIML_SECURITY_AUTH_PROVIDER", "zosmf");
        envVariables.put("ZOWE_IP_ADDRESS", "192.168.1.1");
        envVariables.put("VERIFY_CERTIFICATES", "true");
        envVariables.put("KEYSTORE", "keystore/localhost/localhost.keystore.p12");
        envVariables.put("KEYSTORE_TYPE", "PKCS12");
        envVariables.put("KEYSTORE_PASSWORD", "password");
        envVariables.put("KEY_ALIAS", "localhost");
        envVariables.put("TRUSTSTORE", "keystore/localhost/localhost.truststore.p12");
        envVariables.put("ZWE_DISCOVERY_SERVICES_LIST", "https://localhost:10011/eureka/");
        envVariables.put("WORKSPACE_DIR", "./workspace");
        envVariables.put("APIML_MAX_CONNECTIONS_PER_ROUTE", "10");
        envVariables.put("APIML_MAX_TOTAL_CONNECTIONS", "100");
        envVariables.put("APIML_CORS_ENABLED", "true");
        envVariables.put("APIML_SECURITY_ZOSMF_JWT_AUTOCONFIGURATION_MODE", "JWT");
        envVariables.put("STATIC_DEF_CONFIG_DIR", "config/local/api-defs");
        envVariables.put("APIML_GATEWAY_INTERNAL_ENABLED", "true");
        envVariables.put("APIML_GATEWAY_INTERNAL_PORT", "10017");
        envVariables.put("APIML_GATEWAY_INTERNAL_SSL_KEY_ALIAS", "localhost-multi");
        envVariables.put("APIML_GATEWAY_INTERNAL_SSL_KEYSTORE", "keystore/localhost/localhost-multi.keystore.p12");
        envVariables.put("APIML_DIAG_MODE_ENABLED", "diag");
        builder1.directory(new File("../"));
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        executorService.submit(() -> executeCommand(builder1));
//        executeCommand(builder1);
    }

    private void executeCommand(ProcessBuilder pb) {
        try {
            Process terminalCommandProcess = pb.start();

            InputStream inputStream = terminalCommandProcess.getInputStream();
            BufferedReader br = new BufferedReader(
                new InputStreamReader(inputStream));
            String line;
            int i = 0;
            while ((line = br.readLine()) != null) {
                System.out.println("Line: " + line);
                if (line.startsWith("pid")) {
                    this.subprocessPid = line.substring(line.indexOf("=") + 1);
                    System.out.println("found " + this.subprocessPid);
                }
                i++;
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
