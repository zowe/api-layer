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
import org.apache.commons.lang3.StringUtils;
import org.zowe.apiml.startup.impl.ApiMediationLayerStartupChecker;
import org.zowe.apiml.util.config.ConfigReader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

//TODO this class doesn't lend itself well to switching of configurations.
//attls is integrated in a kludgy way, and deserves a rewrite

@Slf4j
public class FullApiMediationLayer {
    private RunningService discoveryService;
    private RunningService gatewayService;
    private RunningService apiCatalogService;
    private RunningService cachingService;
    private RunningService mockZosmfService;
    private RunningService discoverableClientService;
    private RunningService zaasService;
    private RunningService apimlService;

    private ProcessBuilder nodeJsBuilder;
    private Process nodeJsSampleApp;

    private boolean firstCheck = true;
    private final Map<String, String> env;
    private static final boolean attlsEnabled = "true".equals(System.getProperty("environment.attls"));
    private static final boolean IS_MODULITH_ENABLED = Boolean.parseBoolean(System.getProperty("environment.modulith"));

    private static final FullApiMediationLayer instance = new FullApiMediationLayer();


    private FullApiMediationLayer() {
        env = ConfigReader.environmentConfiguration().getInstanceEnv();

        prepareCaching();
        prepareCatalog();
        prepareDiscoverableClient();
        prepareGateway();
        prepareMockServices();
        prepareDiscovery();
        if (!IS_MODULITH_ENABLED) {
            prepareZaas();
        }
        prepareApiml();
        if (!attlsEnabled) {
            prepareNodeJsSampleApp();
        }
    }

    private void prepareNodeJsSampleApp() {
        List<String> parameters = new ArrayList<>();

        // If NODE_HOME is defined in environment variable, use it, otherwise assume in PATH
        String path = Optional.ofNullable(System.getenv("NODE_HOME"))
                .map(javaHome -> javaHome + "/bin/")
                .orElse("");
        parameters.add(path + "node");
        parameters.add("src/index.js");

        ProcessBuilder builder1 = new ProcessBuilder(parameters);
        builder1.directory(new File("../onboarding-enabler-nodejs-sample-app/"));
        nodeJsBuilder = builder1.inheritIO();
    }

    private void prepareDiscovery() {
        discoveryService = new RunningService("discovery", "discovery-service/build/libs", null, null);
    }

    private void prepareGateway() {
        gatewayService = new RunningService("gateway", "gateway-service/build/libs", null, null);
    }

    private void prepareCatalog() {
        apiCatalogService = new RunningService("apicatalog", "api-catalog-services/build/libs", null, null);
    }

    public void prepareCaching() {
        cachingService = new RunningService("cachingservice", "caching-service/build/libs", null, null);
    }

    public void prepareZaas() {
        zaasService = new RunningService("zaas", "zaas-service/build/libs", null, null);
    }

    /**
     * Modulith
     */
    public void prepareApiml() {
        apimlService = new RunningService("apiml", "apiml/build/libs", null, null);
    }

    private void prepareMockServices() {
        Map<String, String> before = new HashMap<>();
        Map<String, String> after = new HashMap<>();
        if (attlsEnabled) {
            before.put("-Dspring.profiles.active", "attls");
        }
        mockZosmfService = new RunningService("ibmzosmf", "mock-services/build/libs/mock-services.jar", before, after);
    }

    private void prepareDiscoverableClient() {
        Map<String, String> before = new HashMap<>();
        Map<String, String> after = new HashMap<>();
        if (attlsEnabled) {
            before.put("-Dspring.profiles.active", "attls");
        }

        after.put("--spring.config.additional-location", "file:./config/local/discoverable-client.yml");

        discoverableClientService = new RunningService("discoverableclient", "discoverable-client/build/libs/discoverable-client.jar", before, after);
    }

    public static FullApiMediationLayer getInstance() {
        return instance;
    }

    public void start() {
        try {
            var discoveryEnv = new HashMap<>(env);
            discoveryEnv.put("ZWE_configs_port", "10011");
            discoveryService.startWithScript("discovery-package/src/main/resources/bin", discoveryEnv);
            var gatewayEnv = new HashMap<>(env);
            gatewayEnv.put("ZWE_configs_port", "10010");
            gatewayService.startWithScript("gateway-package/src/main/resources/bin", gatewayEnv);
            var catalogEnv = new HashMap<>(env);
            catalogEnv.put("ZWE_configs_port", "10014");
            apiCatalogService.startWithScript("api-catalog-package/src/main/resources/bin", catalogEnv);
            var cachingEnv = new HashMap<>(env);
            cachingEnv.put("ZWE_configs_port", "10016");
            cachingService.startWithScript("caching-service-package/src/main/resources/bin", cachingEnv);
            var zaasEnv = new HashMap<>(env);
            zaasEnv.put("ZWE_configs_port", "10023");
            zaasService.startWithScript("zaas-package/src/main/resources/bin", zaasEnv);
            var apimlModulithEnv = new HashMap<>(env);
            apimlModulithEnv.put("ZWE_configs_port", "10020");
            apimlModulithEnv.put("ZWE_configs_internal_discovery_port", "10021");
            apimlService.startWithScript("apiml-package/src/main/resources/bin", apimlModulithEnv);

            if (!attlsEnabled) {
                nodeJsSampleApp = nodeJsBuilder.start();
            }
            discoverableClientService.start();
            mockZosmfService.start();
            log.info("Services started");
        } catch (IOException ex) {
            log.error("error while starting services: " + ex.getMessage(), ex.getCause());
        }
    }

    @SuppressWarnings("unused")
    // This method would need to handle situations where there are too many environment variables
    private String formatEnv() {
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, String> entry : env.entrySet()) {
            out.append(entry.getKey())
                .append("=")
                .append("\"").append(entry.getValue()).append("\"")
                .append(" ");
        }
        return out.toString();
    }

    public void stop() {
        try {
            discoveryService.stop();
            gatewayService.stop();
            mockZosmfService.stop();

            apiCatalogService.stop();
            discoverableClientService.stop();

            cachingService.stop();
            zaasService.stop();
            if (!attlsEnabled && startServices()) {
                nodeJsSampleApp.destroy();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean startServices() {
        String startServices = System.getProperty("environment.startServices");
        return StringUtils.isNotEmpty(startServices) && Boolean.parseBoolean(startServices);
    }

    public void waitUntilReady() {
        if (firstCheck) {
            new ApiMediationLayerStartupChecker().waitUntilReady();

            firstCheck = false;
        }
    }
}
